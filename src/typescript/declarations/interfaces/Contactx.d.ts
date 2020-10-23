declare module 'cordova-plugin-contacts-x' {

  interface ContactX {
    id: string;

    /**
     * android only
     */
    displayName: string;

    /**
     * name prefix of the contact
     */
    namePrefix: string;

    /**
     * first name (given name) of the contact
     */
    firstName: string;

    /**
     * middle name of the contact
     */
    middleName: string;

    /**
     * family name of the contact
     */
    familyName: string;

    /**
     * name suffix of the contact
     */
    nameSuffix: string;

    /**
     * job title of the contact
     */
    jobTitle: string;

    /**
     * organization name of the contact
     */
    organizationName: string;


    /**
     * unformatted phone-numbers of the contact with labels
     */
    phoneNumbers: {label: string, value: string}[];

    /**
     * unformatted email addresses of the contact with labels
     */
    emailAddresses: {label: string, value: string}[];

    /**
     * unformatted postal addresses of the contact with labels
     */
    postalAddresses: {label: string, value: {street: string, city: string, state: string, postalCode: string, isoCountryCode: string}}[];

    /**
     * unformatted url addresses of the contact with labels
     */
    urlAddresses: {label: string, value: string}[];
  }
}
