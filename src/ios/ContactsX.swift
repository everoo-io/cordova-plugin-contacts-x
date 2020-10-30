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
                                   CNContactPhoneNumbersKey,
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

    @objc(create:)
    func create(command: CDVInvokedUrlCommand) {
        _callbackId = command.callbackId;

        let createContactData: NSDictionary = command.argument(at: 0)! as! NSDictionary

        self.commandDelegate.run {
            let store = CNContactStore()
            self.hasPermission { (granted) in
                guard granted else {
                    self.returnError(error: ErrorCodes.PermissionDenied);
                    return;
                }

                let contact = CNMutableContact();

                // set name prefix
                if let namePrefix = createContactData.value(forKey: "namePrefix") as? String {
                    contact.namePrefix = namePrefix
                }

                // set first name
                if let firstName = createContactData.value(forKey: "firstName") as? String {
                    contact.givenName = firstName
                }

                // set middle name
                if let middleName = createContactData.value(forKey: "middleName") as? String {
                    contact.middleName = middleName
                }

                // set family name
                if let familyName = createContactData.value(forKey: "familyName") as? String {
                    contact.familyName = familyName
                }

                // set name suffix
                if let nameSuffix = createContactData.value(forKey: "nameSuffix") as? String {
                    contact.nameSuffix = nameSuffix
                }

                // set job title
                if let jobTitle = createContactData.value(forKey: "jobTitle") as? String {
                    contact.jobTitle = jobTitle
                }

                // set organization name
                if let organizationName = createContactData.value(forKey: "organizationName") as? String {
                    contact.organizationName = organizationName
                }

                // set postalAddresses
                if let postalAddresses = createContactData.value(forKey: "postalAddresses") as? NSArray {
                    for addressData in postalAddresses {
                        let container = addressData as! NSDictionary
                        let postalAddress = CNMutablePostalAddress()
                        if let value = container.value(forKey: "value") as? NSDictionary {
                            if let street = value.value(forKey: "street") as? String {
                                postalAddress.street = street
                            }
                            if let city = value.value(forKey: "city") as? String {
                                postalAddress.city = city
                            }
                            if let state = value.value(forKey: "state") as? String {
                                postalAddress.state = state
                            }
                            if let postalCode = value.value(forKey: "postalCode") as? String {
                                postalAddress.postalCode = postalCode
                            }
                            if let isoCountryCode = value.value(forKey: "isoCountryCode") as? String {
                                postalAddress.isoCountryCode = isoCountryCode
                            }
                        }

                        let label = container.value(forKey: "label") as? String ?? CNLabelHome
                        contact.postalAddresses.append(CNLabeledValue(label: label, value: postalAddress))
                    }
                }

                // set emailAddresses
                if let emailAddresses = createContactData.value(forKey: "emailAddresses") as? NSArray {
                    for addressData in emailAddresses {
                        let container = addressData as! NSDictionary
                        if let value = container.value(forKey: "value") as? NSString, let label = container.value(forKey: "label") as? String {
                            contact.emailAddresses.append(CNLabeledValue(label: label, value: value))
                        }
                    }
                }

                // set urlAddresses
                if let urlAddresses = createContactData.value(forKey: "urlAddresses") as? NSArray {
                    for addressData in urlAddresses {
                        let container = addressData as! NSDictionary
                        if let value = container.value(forKey: "value") as? NSString, let label = container.value(forKey: "label") as? String {
                            contact.urlAddresses.append(CNLabeledValue(label: label, value: value))
                        }
                    }
                }

                // set phoneNumbers
                if let phoneNumbers = createContactData.value(forKey: "phoneNumbers") as? NSArray {
                    for addressData in phoneNumbers {
                        let container = addressData as! NSDictionary
                        if let value = container.value(forKey: "value") as? NSString, let label = container.value(forKey: "label") as? String {
                            contact.phoneNumbers.append(CNLabeledValue(label: label, value: CNPhoneNumber(stringValue: value as String)))
                        }
                    }
                }

                // set birthday
                if let birthday = createContactData.value(forKey: "birthday") as? String {
                    let dateFormatter = DateFormatter()
                    dateFormatter.dateFormat = "yyyy-MM-dd"
                    dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
                    let date = dateFormatter.date(from: birthday)
                    let calendar = Calendar.current
                    let components = calendar.dateComponents([.year, .month, .day], from: date!)
                    contact.birthday = components
                }

                // set note
                if let note = createContactData.value(forKey: "note") as? String {
                    contact.note = note
                }

                // create image
                if let encodedImage = createContactData.value(forKey: "image") as? String {
                    let dataComponents = encodedImage.components(separatedBy: ",")
                    let mimeType = dataComponents[0]
                    let dataString = dataComponents[1]
                    let dataDecoded = Data(base64Encoded: dataString, options: Data.Base64DecodingOptions.ignoreUnknownCharacters)!
                    let decodedImage: UIImage = UIImage(data: dataDecoded)!
                    if mimeType == "data:image/jpeg;base64" {
                        contact.imageData = decodedImage.jpegData(compressionQuality: 1.0)
                    } else if mimeType == "data:image/png;base64" {
                        contact.imageData = decodedImage.jpegData(compressionQuality: 1.0)
                    }
                }

                let saveRequest = CNSaveRequest()
                saveRequest.add(contact, toContainerWithIdentifier: nil)

                do {
                    try store.execute(saveRequest)

                    // cordove result
                    let result:CDVPluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: contact.identifier);
                    self.commandDelegate.send(result, callbackId: self._callbackId)
                } catch let error {
                    self.returnError(error: ErrorCodes.UnknownError, message: error.localizedDescription)
                    return;
                }
            }
        }
    }

    @objc(delete:)
    func delete(command: CDVInvokedUrlCommand) {
        _callbackId = command.callbackId;

        let contactId: NSString = command.argument(at: 0)! as! NSString

        self.commandDelegate.run {
            let store = CNContactStore()
            self.hasPermission { (granted) in
                guard granted else {
                    self.returnError(error: ErrorCodes.PermissionDenied);
                    return;
                }

                let saveRequest = CNSaveRequest()
                do {
                    let predicate = CNContact.predicateForContacts(withIdentifiers: [contactId as String])
                    let keysToFetch = [CNContactGivenNameKey] as [CNKeyDescriptor]
                    let contacts = try store.unifiedContacts(matching: predicate, keysToFetch: keysToFetch)
                    for contact in contacts {
                        let contactCopy: CNMutableContact = (contact as CNContact).mutableCopy() as! CNMutableContact
                        saveRequest.delete(contactCopy)
                        try store.execute(saveRequest)
                        break
                    }

                    // cordove result
                    let result:CDVPluginResult = CDVPluginResult(status: CDVCommandStatus_OK);
                    self.commandDelegate.send(result, callbackId: self._callbackId)

                } catch let error {
                    self.returnError(error: ErrorCodes.UnknownError, message: error.localizedDescription)
                    return;
                }
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
            @unknown default:
                self.returnError(error: ErrorCodes.UnknownError, message: "")
                return;
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
