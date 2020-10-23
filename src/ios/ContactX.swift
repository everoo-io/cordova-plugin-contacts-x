import Contacts

class ContactX {

    var contact: CNContact;

    init(contact: CNContact) {
        self.contact = contact
    }

    /**
     Returns PostalAddresses
     */
    func getPostalAddresses() -> [NSDictionary] {
        let postalAddresses: [NSDictionary] = self.contact.postalAddresses.map { (ob: CNLabeledValue<CNPostalAddress>) -> NSDictionary in
            return [
                "label": ob.label ?? "private",
                "value": [
                    "street": ob.value.street,
                    "city": ob.value.city,
                    "state": ob.value.state,
                    "postalCode": ob.value.postalCode,
                    "isoCountryCode": ob.value.isoCountryCode
                ]
            ]
        }
        return postalAddresses;
    }

    /**
     Returns LabeledValues
     */
    func getLabeledValues(from: [CNLabeledValue<NSString>]) -> [NSDictionary] {
        let labeledValues: [NSDictionary] = from.map { (ob: CNLabeledValue<NSString>) -> NSDictionary in
            return [
                "label": ob.label ?? "",
                "value": ob.value
            ]
        }
        return labeledValues;
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
        let phoneNumbers: [NSDictionary] = self.contact.phoneNumbers.map { (ob: CNLabeledValue<CNPhoneNumber>) -> NSDictionary in
            return [
                "label": ob.label ?? "private",
                "value": ob.value.stringValue
            ]
        }
        return phoneNumbers;
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

