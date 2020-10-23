/// <reference path="./interfaces/error.d.ts" />
/// <reference path="./interfaces/permission-result.d.ts" />
/// <reference path="./interfaces/ContactX.d.ts" />

declare module 'cordova-plugin-contacts-x' {

  export default class ContactsX {

    static ErrorCodes: {
      UnsupportedAction,
      WrongJsonObject,
      PermissionDenied,
      UnknownError
    }

    /**
     * Get all contacts
     *
     * @param success
     * @param error
     */
    static find(success: (result: ContactX[]) => void, error: (error: ContactXError) => void);

    /**
     * Create a new contact
     *
     * @param success
     * @param error
     */
    static create(success: (result: ContactX[]) => void, error: (error: ContactXError) => void, contactData: any);

    /**
     * Deletes a contact
     *
     * @param success
     * @param error
     */
    static delete(success: (result: ContactX[]) => void, error: (error: ContactXError) => void, contactIdentifier: string);

    /**
     * Check permission is available
     *
     * @param success
     * @param error
     */
    static hasPermission(success: (result: ContactXPermissionResult) => void, error: (error: ContactXError) => void);

    /**
     * Request Permission (if not available and possible)
     *
     * @param success
     * @param error
     */
    static requestPermission(success: (result: ContactXPermissionResult) => void, error: (error: ContactXError) => void);
  }

}
