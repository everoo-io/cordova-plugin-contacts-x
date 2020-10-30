package de.einfachhans.ContactsX;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Base64InputStream;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * This class echoes a string called from JavaScript.
 */
public class ContactsX extends CordovaPlugin {

    private CallbackContext _callbackContext;

    public static final String READ = Manifest.permission.READ_CONTACTS;
    public static final String WRITE = Manifest.permission.WRITE_CONTACTS;
    public static final String GET_ACCOUNTS = Manifest.permission.GET_ACCOUNTS;

    public static final int REQ_CODE_READ = 0;

    private static final long MAX_PHOTO_SIZE = 1048576;

    private static final String ASSET_URL_PREFIX = "file:///android_asset/";

    private static final String EMAIL_REGEXP = ".+@.+\\.+.+"; /* <anything>@<anything>.<anything>*/

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
            } else if (action.equals("create")) {
                if (PermissionHelper.hasPermission(this, WRITE)) {
                    this.create(args.getJSONObject(0));
                } else {
                    returnError(ContactsXErrorCodes.PermissionDenied);
                }
            } else if (action.equals("delete")) {
                if (PermissionHelper.hasPermission(this, READ) && PermissionHelper.hasPermission(this, WRITE)) {
                    this.delete(args.getString(0));
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
                    ContactsContract.CommonDataKinds.Contactables.DATA,
            };
            String[] selectionArgs = new String[]{
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
            };
            String questionMarks = "";
            for (int i = 0; i < selectionArgs.length; i++) {
                questionMarks += "?" + (i < selectionArgs.length - 1 ? ", " : "");
            }
            String selection = ContactsContract.Data.MIMETYPE + " in (" + questionMarks + ")";

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
                String contactId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));

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
                    jsContact.put("birthday", "");

                    jsContacts.put(jsContact);
                } else {
                    jsContact = contactsById.get(contactId);
                }

                String mimeType = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Data.MIMETYPE));

                String data = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.DATA));

                assert jsContact != null;
                switch (mimeType) {
                    case ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE:
                        String postalAddressLabel = "";
                        try {
                            int type = contactsCursor.getInt(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
                            if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
                                postalAddressLabel = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL));
                            }
                        } catch (Exception ignored) {}
                        JSONObject postalAddressLabeledValue = new JSONObject();
                        postalAddressLabeledValue.put("label", postalAddressLabel);

                        JSONArray jsPostalAddresses = jsContact.getJSONArray("postalAddresses");
                        try {
                            String street = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                            String city = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                            String state = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
                            String postalCode = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                            String isoCountryCode = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                            JSONObject jsPostalAddress = new JSONObject();
                            jsPostalAddress.put("street", street == null ? "" : street);
                            jsPostalAddress.put("city", city == null ? "" : city);
                            jsPostalAddress.put("state", state == null ? "" : state);
                            jsPostalAddress.put("postalCode", postalCode == null ? "" : postalCode);
                            jsPostalAddress.put("isoCountryCode", isoCountryCode == null ? "" : isoCountryCode);
                            postalAddressLabeledValue.put("value", jsPostalAddress);
                            jsPostalAddresses.put(postalAddressLabeledValue);
                        } catch (Exception ignore) {}
                        break;
                    case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                        String emailAddressLabel = "";
                        try {
                            int type = contactsCursor.getInt(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE));
                            if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
                                emailAddressLabel = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL));
                            }
                        } catch (Exception ignored) {}
                        JSONObject emailAddressLabeledValue = new JSONObject();
                        emailAddressLabeledValue.put("label", emailAddressLabel);

                        JSONArray jsEmailAddresses = jsContact.getJSONArray("emailAddresses");
                        emailAddressLabeledValue.put("value", data);
                        jsEmailAddresses.put(emailAddressLabeledValue);
                        break;
                    case ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE:
                        String urlAddressLabel = "";
                        try {
                            int type = contactsCursor.getInt(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website.TYPE));
                            if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
                                urlAddressLabel = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.LABEL));
                            }
                        } catch (Exception ignored) {}
                        JSONObject urlAddressLabeledValue = new JSONObject();
                        urlAddressLabeledValue.put("label", urlAddressLabel);

                        JSONArray jsUrlAddresses = jsContact.getJSONArray("urlAddresses");
                        urlAddressLabeledValue.put("value", data);
                        jsUrlAddresses.put(urlAddressLabeledValue);
                        break;
                    case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                        String phoneNumberLabel = "";
                        try {
                            int type = contactsCursor.getInt(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                            if (type == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) {
                                phoneNumberLabel = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
                            }
                        } catch (Exception ignored) {}
                        JSONObject phoneNumberLabeledValue = new JSONObject();
                        phoneNumberLabeledValue.put("label", phoneNumberLabel);

                        JSONArray jsPhoneNumbers = jsContact.getJSONArray("phoneNumbers");
                        phoneNumberLabeledValue.put("value", data);
                        jsPhoneNumbers.put(phoneNumberLabeledValue);
                        break;
                    case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                        try {
                            String namePrefix = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PREFIX));
                            String firstName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                            String middleName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
                            String familyName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                            String nameSuffix = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.SUFFIX));
                            jsContact.put("namePrefix", namePrefix == null ? "" : namePrefix);
                            jsContact.put("firstName", firstName == null ? "" : firstName);
                            jsContact.put("middleName", middleName == null ? "" : middleName);
                            jsContact.put("familyName", familyName == null ? "" : familyName);
                            jsContact.put("nameSuffix", nameSuffix == null ? "" : nameSuffix);

                        } catch (IllegalArgumentException ignored) {
                        }
                        break;
                    case ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE:
                        try {
                            String jobTitle = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION));
                            String organizationName = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY));
                            jsContact.put("jobTitle", jobTitle == null ? "" : jobTitle);
                            jsContact.put("organizationName", organizationName == null ? "" : organizationName);
                        } catch (IllegalArgumentException ignored) {
                        }
                        break;
                    case ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE:
                        try {
                            int type = contactsCursor.getInt(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE));
                            if (type == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY) {
                                String dateString = contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE));
                                jsContact.put("birthday", dateString);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                        break;
                }

                contactsById.put(contactId, jsContact);
            }
        }
        return jsContacts;
    }

    private String getJsonString(JSONObject obj, String key) {
        try {
            return obj.getString(key);
        } catch (JSONException ignore) {
            return null;
        }
    }

    private void insertWebsite(ArrayList<ContentProviderOperation> ops,
                               JSONObject website) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Website.DATA, getJsonString(website, "value"))
                .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM)
                .withValue(ContactsContract.CommonDataKinds.Website.LABEL, getJsonString(website, "label"))
                .build());
    }

    private void insertOrganization(ArrayList<ContentProviderOperation> ops,
                                    JSONObject contact) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, getJsonString(contact, "organizationName"))
                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, getJsonString(contact, "jobTitle"))
                .build());
    }

    private void insertAddress(ArrayList<ContentProviderOperation> ops,
                               JSONObject address) {
        JSONObject valueObj = new JSONObject();
        try {
            valueObj = address.getJSONObject("value");
        } catch (JSONException ignore) {}
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM)
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, getJsonString(valueObj, "street"))
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, getJsonString(valueObj, "city"))
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, getJsonString(valueObj, "state"))
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, getJsonString(valueObj, "postalCode"))
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, getJsonString(valueObj, "country"))
                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, getJsonString(address, "label"))
                .build());
    }

    private void insertEmail(ArrayList<ContentProviderOperation> ops,
                             JSONObject email) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, getJsonString(email, "value"))
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM)
                .withValue(ContactsContract.CommonDataKinds.Email.LABEL, getJsonString(email, "label"))
                .build());
    }

    private void insertPhone(ArrayList<ContentProviderOperation> ops,
                             JSONObject phone) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, getJsonString(phone, "value"))
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM)
                .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, getJsonString(phone, "label"))
                .build());
    }

    private void insertPhoto(ArrayList<ContentProviderOperation> ops,
                             String photo) {
        byte[] bytes = getPhotoBytes(photo);
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
                .build());
    }

    private byte[] getPhotoBytes(String filename) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            int bytesRead = 0;
            long totalBytesRead = 0;
            byte[] data = new byte[8192];
            InputStream in = getPathFromUri(filename);

            while ((bytesRead = in.read(data, 0, data.length)) != -1 && totalBytesRead <= MAX_PHOTO_SIZE) {
                buffer.write(data, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            in.close();
            buffer.flush();
        } catch (FileNotFoundException e) {} catch (IOException e) {}
        return buffer.toByteArray();
    }

    private InputStream getPathFromUri(String path) throws IOException {
        if (path.startsWith("data:")) { // data:image/png;base64,[ENCODED_IMAGE]
            String dataInfos = path.substring(0, path.indexOf(','));
            dataInfos = dataInfos.substring(dataInfos.indexOf(':') + 1);
            String baseEncoding = dataInfos.substring(dataInfos.indexOf(';') + 1);
            // [ENCODED_IMAGE]
            if("base64".equalsIgnoreCase(baseEncoding)) {
                String img = path.substring(path.indexOf(',') + 1);
                byte[] encodedData = img.getBytes();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encodedData, 0, encodedData.length);
                Base64InputStream base64InputStream = new Base64InputStream(byteArrayInputStream, Base64.DEFAULT);
                return base64InputStream;
            }
        }
        if (path.startsWith("content:")) {
            Uri uri = Uri.parse(path);
            return this.cordova.getActivity().getContentResolver().openInputStream(uri);
        }

        if (path.startsWith(ASSET_URL_PREFIX)) {
            String assetRelativePath = path.replace(ASSET_URL_PREFIX, "");
            return this.cordova.getActivity().getAssets().open(assetRelativePath);
        }

        if (path.startsWith("http:") || path.startsWith("https:") || path.startsWith("file:")) {
            URL url = new URL(path);
            return url.openStream();
        }

        return new FileInputStream(path);
    }

    private Date getBirthday(JSONObject contact) {
        try {
            String dateString = contact.getString("birthday");
            String[] dateSplit = dateString.split("-");
            Date date = new GregorianCalendar(Integer.parseInt(dateSplit[0]), Integer.parseInt(dateSplit[1]) - 1, Integer.parseInt(dateSplit[2])).getTime();
            return date;
        } catch (Exception e) {
            return null;
        }
    }

    private void create(JSONObject contactData) throws JSONException {
        this.cordova.getThreadPool().execute(() -> {
            AccountManager mgr = AccountManager.get(this.cordova.getActivity());
            Account[] accounts = mgr.getAccounts();
            String accountName = null;
            String accountType = null;

            if (accounts.length == 1) {
                accountName = accounts[0].name;
                accountType = accounts[0].type;
            }
            else if (accounts.length > 1) {
                for (Account a : accounts) {
                    if (a.type.contains("eas") && a.name.matches(EMAIL_REGEXP)) /*Exchange ActiveSync*/{
                        accountName = a.name;
                        accountType = a.type;
                        break;
                    }
                }
                if (accountName == null) {
                    for (Account a : accounts) {
                        if (a.type.contains("com.google") && a.name.matches(EMAIL_REGEXP)) /*Google sync provider*/{
                            accountName = a.name;
                            accountType = a.type;
                            break;
                        }
                    }
                }
                if (accountName == null) {
                    for (Account a : accounts) {
                        if (a.name.matches(EMAIL_REGEXP)) /*Last resort, just look for an email address...*/{
                            accountName = a.name;
                            accountType = a.type;
                            break;
                        }
                    }
                }
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            //Add contact type
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                    .build());

            // Add name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, getJsonString(contactData, "familyName"))
            .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, getJsonString(contactData, "middleName"))
            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, getJsonString(contactData, "firstName"))
            .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, getJsonString(contactData, "namePrefix"))
            .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, getJsonString(contactData, "nameSuffix"))
            .build());

            // Add organizations
            insertOrganization(ops, contactData);

            //Add phone numbers
            JSONArray phones = null;
            try {
                phones = contactData.getJSONArray("phoneNumbers");
                if (phones != null) {
                    for (int i = 0; i < phones.length(); i++) {
                        if(!phones.isNull(i)){
                            JSONObject phone = (JSONObject) phones.get(i);
                            insertPhone(ops, phone);
                        }
                    }
                }
            } catch (JSONException e) {}

            // Add emails
            JSONArray emails = null;
            try {
                emails = contactData.getJSONArray("emailAddresses");
                if (emails != null) {
                    for (int i = 0; i < emails.length(); i++) {
                        JSONObject email = (JSONObject) emails.get(i);
                        insertEmail(ops, email);
                    }
                }
            } catch (JSONException e) {}

            // Add addresses
            JSONArray addresses = null;
            try {
                addresses = contactData.getJSONArray("postalAddresses");
                if (addresses != null) {
                    for (int i = 0; i < addresses.length(); i++) {
                        JSONObject address = (JSONObject) addresses.get(i);
                        insertAddress(ops, address);
                    }
                }
            } catch (JSONException e) {}

            // Add urls
            JSONArray websites = null;
            try {
                websites = contactData.getJSONArray("urlAddresses");
                if (websites != null) {
                    for (int i = 0; i < websites.length(); i++) {
                        JSONObject website = (JSONObject) websites.get(i);
                        insertWebsite(ops, website);
                    }
                }
            } catch (JSONException e) {}

            // Add birthday
            Date birthday = getBirthday(contactData);
            if (birthday != null) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
                        .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, birthday.toString())
                        .build());
            }

            // Add photos
            try {
                String image = contactData.getString("image");
                if (image != null) {
                    insertPhoto(ops, image);
                }
            } catch (JSONException e) {}

            // Add note
            String note = getJsonString(contactData, "note");
            if (note != null) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, note)
                        .build());
            }

            String newId = null;

            //Add contact
            try {
                ContentProviderResult[] cpResults = this.cordova.getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                if (cpResults.length >= 0) {
                    newId = cpResults[0].uri.getLastPathSegment();
                }
            } catch (RemoteException e) {} catch (OperationApplicationException e) {}

            this._callbackContext.success(newId);
        });
    }

    private void delete(String id) throws JSONException {
        this.cordova.getThreadPool().execute(() -> {
            int result = 0;
            Cursor cursor = this.cordova.getActivity().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    null,
                    ContactsContract.Contacts._ID + " = ?",
                    new String[] { id }, null);

            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                result = this.cordova.getActivity().getContentResolver().delete(uri, null, null);
            }
            this._callbackContext.success();
        });
    }

    private void hasPermission() throws JSONException {
        JSONObject response = new JSONObject();
        response.put("read", PermissionHelper.hasPermission(this, READ));
        response.put("write", PermissionHelper.hasPermission(this, WRITE));
        response.put("getAccounts", PermissionHelper.hasPermission(this, GET_ACCOUNTS));
        if (this._callbackContext != null) {
            this._callbackContext.success(response);
        }
    }

    private void requestPermission(int requestCode) {
        PermissionHelper.requestPermission(this, requestCode, GET_ACCOUNTS);
        PermissionHelper.requestPermission(this, requestCode, READ);
        PermissionHelper.requestPermission(this, requestCode, WRITE);
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
