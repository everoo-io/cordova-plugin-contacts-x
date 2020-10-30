package de.einfachhans.ContactsX;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * This class echoes a string called from JavaScript.
 */
public class ContactsX extends CordovaPlugin {

    private CallbackContext _callbackContext;

    public static final String READ = Manifest.permission.READ_CONTACTS;

    public static final int REQ_CODE_READ = 0;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this._callbackContext = callbackContext;

        try {
            if (action.equals("find")) {
                if (PermissionHelper.hasPermission(this, READ)) {
                    this.find();
                } else {
                    returnError(ContactsXErrorCodes.PermissionDenied);
                }
            } else if (action.equals("hasPermission")) {
                this.hasPermission();
            } else if (action.equals("requestPermission")) {
                this.requestPermission(REQ_CODE_READ);
            } else {
                returnError(ContactsXErrorCodes.UnsupportedAction);
            }
        } catch (JSONException exception) {
            returnError(ContactsXErrorCodes.WrongJsonObject);
        } catch (Exception exception) {
            returnError(ContactsXErrorCodes.UnknownError, exception.getMessage());
        }

        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        this.hasPermission();
    }

    private void find() throws JSONException {
        this.cordova.getThreadPool().execute(() -> {

            ContentResolver contentResolver = this.cordova.getContext().getContentResolver();

            String[] projection = new String[]{
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.Contacts._ID,
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.PREFIX,
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.SUFFIX,
                    ContactsContract.CommonDataKinds.Organization.COMPANY,
                    ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION,
                    ContactsContract.CommonDataKinds.Contactables.DATA,
            };
            String selection = ContactsContract.Data.MIMETYPE + " in (?, ?, ?, ?, ?)";
            String[] selectionArgs = new String[]{
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            };

            Cursor contactsCursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
            );

            JSONArray result = null;
            try {
                result = handleFindResult(contactsCursor);
            } catch (JSONException e) {
                this.returnError(ContactsXErrorCodes.UnknownError, e.getMessage());
            }

            if (contactsCursor != null) {
                contactsCursor.close();
            }

            this._callbackContext.success(result);
        });
    }

    private JSONArray handleFindResult(Cursor contactsCursor) throws JSONException {
        // initialize array
        JSONArray jsContacts = new JSONArray();

        if (contactsCursor != null && contactsCursor.getCount() > 0) {
            HashMap<Object, JSONObject> contactsById = new HashMap<>();

            while (contactsCursor.moveToNext()) {
                String contactId = contactsCursor.getString(
                        contactsCursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                );

                JSONObject jsContact = new JSONObject();

                if (!contactsById.containsKey(contactId)) {
                    // this contact does not yet exist in HashMap,
                    // so put it to the HashMap

                    jsContact.put("id", contactId);
                    String displayName = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    jsContact.put("displayName", displayName);
                    JSONArray jsPostalAddresses = new JSONArray();
                    jsContact.put("postalAddresses", jsPostalAddresses);
                    JSONArray jsEmailAddresses = new JSONArray();
                    jsContact.put("emailAddresses", jsEmailAddresses);
                    JSONArray jsUrlAddresses = new JSONArray();
                    jsContact.put("urlAddresses", jsUrlAddresses);
                    JSONArray jsPhoneNumbers = new JSONArray();
                    jsContact.put("phoneNumbers", jsPhoneNumbers);

                    jsContacts.put(jsContact);
                } else {
                    jsContact = contactsById.get(contactId);
                }

                String mimeType = contactsCursor.getString(
                        contactsCursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                );
                contactsCursor.get
                String type = '';
                String label = '';
                try {
                    type = contactsCursor.getString(
                          contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.CommonColumns.TYPE)
                    );
                    if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
                        label = contactsCursor.getString(
                                contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.CommonColumns.LABEL)
                        );
                    }
                } catch (Exception ignored) {}

                String data = contactsCursor.getString(
                        contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.DATA)
                );
                JSONObject labeledValue = new JSONObject();
                labeledValue.put("label", label);

                assert jsContact != null;
                switch (mimeType) {
                    case ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE:
                        JSONArray jsPostalAddresses = jsContact.getJSONArray("postalAddresses");
                        String street = contactsCursor.getString(
                                contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
                        );
                        String city = contactsCursor.getString(
                                contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
                        );
                        String state = contactsCursor.getString(
                                contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
                        );
                        String postalCode = contactsCursor.getString(
                                contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
                        );
                        String isoCountryCode = contactsCursor.getString(
                                contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
                        );
                        JSONObject jsPostalAddress = new JSONObject();
                        jsPostalAddress.put("street", street);
                        jsPostalAddress.put("city", city);
                        jsPostalAddress.put("state", state);
                        jsPostalAddress.put("postalCode", postalCode);
                        jsPostalAddress.put("isoCountryCode", isoCountryCode);
                        labeledValue.put("value", jsPostalAddress);
                        jsPostalAddresses.put(labeledValue);
                        break;
                    case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                        JSONArray jsEmailAddresses = jsContact.getJSONArray("emailAddresses");
                        labeledValue.put("value", data);
                        jsEmailAddresses.put(labeledValue);
                        break;
                    case ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE:
                        JSONArray jsUrlAddresses = jsContact.getJSONArray("urlAddresses");
                        labeledValue.put("value", data);
                        jsUrlAddresses.put(labeledValue);
                        break;
                    case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                        JSONArray jsPhoneNumbers = jsContact.getJSONArray("phoneNumbers");
                        labeledValue.put("value", data);
                        jsPhoneNumbers.put(labeledValue);
                        break;
                    case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                        try {
                            String namePrefix = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PREFIX));
                            String firstName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                            String middleName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
                            String familyName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                            String nameSuffix = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.SUFFIX));
                            String jobTitle = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION));
                            String organizationName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY));
                            jsContact.put("namePrefix", firstName);
                            jsContact.put("firstName", firstName);
                            jsContact.put("middleName", middleName);
                            jsContact.put("familyName", familyName);
                            jsContact.put("nameSuffix", nameSuffix);
                            jsContact.put("jobTitle", jobTitle);
                            jsContact.put("organizationName", organizationName);
                        } catch (IllegalArgumentException ignored) {
                        }
                        break;
                }

                contactsById.put(contactId, jsContact);
            }
        }
        return jsContacts;
    }

    private void hasPermission() throws JSONException {
        JSONObject response = new JSONObject();
        response.put("read", PermissionHelper.hasPermission(this, READ));
        if (this._callbackContext != null) {
            this._callbackContext.success(response);
        }
    }

    private void requestPermission(int requestCode) {
        PermissionHelper.requestPermission(this, requestCode, READ);
    }

    private void returnError(ContactsXErrorCodes errorCode) {
        returnError(errorCode, null);
    }

    private void returnError(ContactsXErrorCodes errorCode, String message) {
        if (_callbackContext != null) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("code", errorCode.value);
            resultMap.put("message", message == null ? "" : message);
            _callbackContext.error(new JSONObject(resultMap));
            _callbackContext = null;
        }
    }
}
