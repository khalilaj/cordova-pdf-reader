var exec = require("cordova/exec");

var JS_HANDLE = "SUBSMuPDFIntegration";
var CDV_HANDLE = "SUBSMuPDFIntegration";

var CDV_HANDLE_ACTIONS = {
  GET_SUPPORT_INFO: "getSupportInfo",

  VIEW_DOCUMENT: "viewDocument"
};

function getOptions(provided) {
  var options = provided || {};

  if (!options.documentView) options.documentView = {};
  if (!options.documentView.closeLabel)
    options.documentView.closeLabel = "Done";

  if (!options.navigationView) options.navigationView = {};
  if (!options.navigationView.closeLabel)
    options.navigationView.closeLabel = "Back";

  if (!options.android) options.android = {};

  return options;
}

var SUBSMuPDFIntegration = {
  viewDocument: function(url, contentType, options, onShow, onClose, onError) {
    var me = this;
    var errorPrefix = "Error in " + JS_HANDLE + ".viewDocument(): ";
    try {
      options = getOptions(options);

      if (!options.title) options.title = url.split("/").pop(); // strip file name from url

      exec(
        function() {
          if (onShow) {
            onShow();
          } else if (onClose) {
            onClose();
          } else {
            var errorMsg = "unknown state";
          }
        },
        function(err) {
          if (onError) onError(err);
        },
        CDV_HANDLE,
        CDV_HANDLE_ACTIONS.VIEW_DOCUMENT,
        [{ url: url, contentType: contentType, options: options }]
      );
    } catch (e) {
      if (onError) onError(e);
    }
  }
};

module.exports = SUBSMuPDFIntegration;
