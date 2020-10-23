import Contacts
import DateFormatter

class ContactX {

    var contact: CNContact;

    init(contact: CNContact) {
        self.contact = contact
    }

    func getJson() -> NSDictionary {

        /*let postalAddresses: [String] = self.contact.postalAddresses.map { (ob: CNLabeledValue<CNPostalAddress>) -> String in
            return ob.value.stringValue
        }*/
        let postalAddresses = [];

        let emailAddresses: [String] = self.contact.emailAddresses.map { (ob: CNLabeledValue<CNLabeledValue>) -> String in
            return ob.value.stringValue
        }

        let urls: [String] = self.contact.urls.map { (ob: CNLabeledValue<CNLabeledValue>) -> String in
            return ob.value.stringValue
        }

        let phoneNumbers: [String] = self.contact.phoneNumbers.map { (ob: CNLabeledValue<CNPhoneNumber>) -> String in
            return ob.value.stringValue
        }

        let birthday = nil;
        if self.contact.birthday != nil {
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "dd-MMM-yyyy"
            birthday = dateFormatter.string(from: self.contact.birthday)
        }

        return [
            "id": self.contact.identifier,
            "namePrefix": self.contact.namePrefix,
            "firstName": self.contact.givenName,
            "middleName": self.contact.middleName,
            "familyName": self.contact.familyName,
            "nameSuffix": self.contact.nameSuffix,
            "jobTitle": self.contact.jobTitle,
            "organizationName": self.contact.organizationName,
            "postalAddresses": postalAddresses,
            "emailAddresses": emailAddresses,
            "urls": urls,
            "phoneNumbers": phoneNumbers,
            "birthday": birthday,
            "hasImage": self.contact.imageDataAvailable//,
            //"note": self.contact.note
        ];
    }
}
