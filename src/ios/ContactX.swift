import Contacts

class ContactX {

    /**
    @var internal contact variable
     */
    var contact: CNContact;

    /**
     Constructor
     */
    init(contact: CNContact) {
        self.contact = contact
    }

    /**
     Returns the json structure for a CNLabeledValue for generic type NSString
     */
    func getLabeledValueDictionary(obj: CNLabeledValue<NSString>) -> NSDictionary {
        return [
            "label": obj.label ?? "",
            "value": obj.value
        ]
    }

    /**
     Returns the json structure for a CNLabeledValue for generic type CNPhoneNumber
     */
    func getLabeledValueDictionary(obj: CNLabeledValue<CNPhoneNumber>) -> NSDictionary {
        return [
            "label": obj.label ?? "",
            "value": obj.value.stringValue
        ]
    }

    /**
     Returns the json structure for a CNLabeledValue for generic type CNPostalAddress
     */
    func getLabeledValueDictionary(obj: CNLabeledValue<CNPostalAddress>) -> NSDictionary {
        return [
            "label": obj.label ?? "",
            "value": [
                "street": obj.value.street,
                "city": obj.value.city,
                "state": obj.value.state,
                "postalCode": obj.value.postalCode,
                "isoCountryCode": obj.value.isoCountryCode
            ]
        ]
    }

    /**
     Returns LabeledValues for generic type NSString
     */
    func getLabeledValues(from: [CNLabeledValue<NSString>]) -> [NSDictionary] {
        let labeledValues: [NSDictionary] = from.map { (ob: CNLabeledValue<NSString>) -> NSDictionary in
            return self.getLabeledValueDictionary(obj: ob)
        }
        return labeledValues;
    }

    /**
     Returns LabeledValues for generic type CNPhoneNumbers
     */
    func getLabeledValues(from: [CNLabeledValue<CNPhoneNumber>]) -> [NSDictionary] {
        let labeledValues: [NSDictionary] = from.map { (ob: CNLabeledValue<CNPhoneNumber>) -> NSDictionary in
            return self.getLabeledValueDictionary(obj: ob)
        }
        return labeledValues;
    }

    /**
     Returns LabeledValues for generic type CNPostalAddress
     */
    func getLabeledValues(from: [CNLabeledValue<CNPostalAddress>]) -> [NSDictionary] {
        let labeledValues: [NSDictionary] = from.map { (ob: CNLabeledValue<CNPostalAddress>) -> NSDictionary in
            return self.getLabeledValueDictionary(obj: ob)
        }
        return labeledValues;
    }

    /**
     Returns PostalAddresses
     */
    func getPostalAddresses() -> [NSDictionary] {
        return self.getLabeledValues(from: self.contact.emailAddresses)
    }

    /**
     Returns all email addresses
     */
    func getEmailAddresses() -> [NSDictionary] {
        return self.getLabeledValues(from: self.contact.emailAddresses)
    }

    /**
     Returns all url addresses
     */
    func getUrlAddresses() -> [NSDictionary] {
        return self.getLabeledValues(from: self.contact.urlAddresses)
    }

    /**
     Returns all phone numbers
     */
    func getPhoneNumbers() -> [NSDictionary] {
        return self.getLabeledValues(from: self.contact.phoneNumbers)
    }

    /**
     Returns a corresponding date string for the contacts birthday
     */
    func getBirthdayDateString() -> String {
        var birthdayString: String = "";
        if let birthday: Date = self.contact.birthday?.date {
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd"
            dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
            birthdayString = dateFormatter.string(from: birthday)
        }
        return birthdayString;
    }

    /**
     Returns the contact json
     */
    func getJson() -> NSDictionary {
        return [
            "id": self.contact.identifier,
            "namePrefix": self.contact.namePrefix,
            "firstName": self.contact.givenName,
            "middleName": self.contact.middleName,
            "familyName": self.contact.familyName,
            "nameSuffix": self.contact.nameSuffix,
            "jobTitle": self.contact.jobTitle,
            "organizationName": self.contact.organizationName,
            "postalAddresses": self.getPostalAddresses(),
            "emailAddresses": self.getEmailAddresses(),
            "urlAddresses": self.getUrlAddresses(),
            "phoneNumbers": self.getPhoneNumbers(),
            "birthday": self.getBirthdayDateString(),
            "hasImage": self.contact.imageDataAvailable//,
            //"note": self.contact.note
        ];
    }
}

