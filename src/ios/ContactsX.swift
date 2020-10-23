import Contacts

@objc(ContactsX) class ContactsX : CDVPlugin {

    var _callbackId: String?

    @objc(pluginInitialize)
    override func pluginInitialize() {
        super.pluginInitialize();
    }

    @objc(find:)
    func find(command: CDVInvokedUrlCommand) {
        _callbackId = command.callbackId;

        self.commandDelegate.run {
            let store = CNContactStore();
            self.hasPermission { (granted) in
                guard granted else {
                    self.returnError(error: ErrorCodes.PermissionDenied);
                    return;
                }
                var contacts = [ContactX]()
                let keysToFetch = [CNContactNamePrefixKey,
                                   CNContactGivenNameKey,
                                   CNContactMiddleNameKey,
                                   CNContactFamilyNameKey,
                                   CNContactNameSuffixKey,
                                   CNContactJobTitleKey,
                                   CNContactOrganizationNameKey,
                                   CNContactPostalAddressesKey,
                                   CNContactEmailAddressesKey,
                                   CNContactUrlAddressesKey,
                                   CNContactPhoneNumbersKey
                                   CNContactBirthdayKey,
                                   CNContactImageDataAvailableKey//,
                                   //CNContactNoteKey
                                   ]
                let request = CNContactFetchRequest(keysToFetch: keysToFetch as [NSString])

                    do {
                        try store.enumerateContacts(with: request) {
                            (contact, stop) in
                            // Array containing all unified contacts from everywhere
                            contacts.append(ContactX(contact: contact))
                        }
                    }
                    catch let error {
                        self.returnError(error: ErrorCodes.UnknownError, message: error.localizedDescription)
                        return;
                    }

                var resultArray = [] as Array;
                for contact in contacts {
                    resultArray.append(contact.getJson());
                }
                let result:CDVPluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: resultArray);
                self.commandDelegate.send(result, callbackId: self._callbackId)
            }
        }
    }

    @objc(hasPermission:)
    func hasPermission(command: CDVInvokedUrlCommand) {
        _callbackId = command.callbackId;

        self.hasPermission { (granted) in
            let dict = [
                "read": granted
            ];

            let result:CDVPluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: dict);
            self.commandDelegate.send(result, callbackId: self._callbackId)
        }
    }

    @objc(requestPermission:)
    func requestPermission(command: CDVInvokedUrlCommand) {
        _callbackId = command.callbackId

        self.hasPermission(completionHandler: { (granted) in
            let dict = [
                "read": granted
            ];

            let result:CDVPluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: dict);
            self.commandDelegate.send(result, callbackId: self._callbackId)
        }, requestIfNotAvailable: true)
    }

    func hasPermission(completionHandler: @escaping (_ accessGranted: Bool) -> Void, requestIfNotAvailable: Bool = false) {
        let store = CNContactStore();
        switch CNContactStore.authorizationStatus(for: .contacts) {
                case .authorized:
                    completionHandler(true)
                case .denied:
                    completionHandler(false)
                case .restricted, .notDetermined:
                    if(requestIfNotAvailable) {
                        store.requestAccess(for: .contacts) { granted, error in
                            if granted {
                                completionHandler(true)
                            } else {
                                DispatchQueue.main.async {
                                    completionHandler(false)
                                }
                            }
                        }
                    } else {
                        completionHandler(false)
                    }
                }
    }

    func returnError(error: ErrorCodes, message: String = "") {
        if(_callbackId != nil) {
            let result:CDVPluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR, messageAs: [
                    "error": error.rawValue,
                    "message": message
            ]);
            self.commandDelegate.send(result, callbackId: _callbackId)
            _callbackId = nil;
        }
    }
}

enum ErrorCodes:NSNumber {
    case UnsupportedAction = 1
    case WrongJsonObject = 2
    case PermissionDenied = 3
    case UnknownError = 10
}
