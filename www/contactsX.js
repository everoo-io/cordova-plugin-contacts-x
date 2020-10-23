var exec = require('cordova/exec');

var contactsX = {
  ErrorCodes: {
    UnsupportedAction: 1,
    WrongJsonObject: 2,
    PermissionDenied: 3,
    UnknownError: 10
  },

  find: function (success, error) {
    exec(success, error, 'ContactsX', 'find', []);
  },

  create: function (success, error, contactData) {
    exec(success, error, 'ContactsX', 'create', [contactData]);
  },

  delete: function (success, error, contactIdentifier) {
    exec(success, error, 'ContactsX', 'delete', [contactIdentifier]);
  },

  hasPermission: function (success, error) {
    exec(success, error, 'ContactsX', 'hasPermission', []);
  },

  requestPermission: function (success, error) {
    exec(success, error, 'ContactsX', 'requestPermission', []);
  }
}

module.exports = contactsX;
