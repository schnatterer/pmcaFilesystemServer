const saveAs = require('file-saver').saveAs;
const JSZip = require('jszip');

function init(meta) {

  $(document).on("change", ".checkbox-download-day", function (event) {
    $(".card[data-day='" + $(this).data("day") + "'] .checkbox-download")
        .prop("checked", $(this).prop("checked"))
        .trigger("change");
  });

  $(document).on("change", ".checkbox-download", function (event) {
    if ($(".checkbox-download:checked").length > 0) {
      let size = 0;
      $(".checkbox-download:checked").each(function () {
        const el = $(this).parents(".card-link");
        size += parseInt(el.data("size"));
      });
      $("#download").text(`Download ${$(".checkbox-download:checked").length} files - ${bytesToSize(size)}`);
      $("#download").show();
    } else {
      $("#download").hide();
    }
  });
  $("#download").hide();
  $("#download").click(function () {
    $("#zip-progress").css("width", "0%");
    $("#zip-log").text("");
    $("#zipModel").modal({backdrop: "static", keyboard: false});
    const files = [];
    $(".checkbox-download:checked").each(function () {
      const el = $(this).parents(".card-link");
      files.push({name: el.data("name"), file: el.data("href")});
    });

    $("#zip-log").append(
        "Downloading " + files.length + " files as ZIP..." + "\n"
    );

    // Start building a ZIP file
    const zip = new JSZip();

    const promises = [];
    let startedFiles = 0;
    let finishedFiles = 0;
    for (const file of files) {
      promises.push(
          fetch(file.file)
              .then(function (response) {
                appendZipLog("Downloading " + file.name + "...");
                startedFiles++;
                updateZipProgress(files.length, startedFiles, finishedFiles, 0);
                // 2) filter on 200 OK
                if (response.status === 200 || response.status === 0) {
                  return Promise.resolve(response.blob());
                } else {
                  return Promise.reject(new Error(response.statusText));
                }
              })
              .then(function (blob) {
                appendZipLog("Added " + file.name + " to zip");
                zip.file(file.name, blob);
                finishedFiles++;
                updateZipProgress(files.length, startedFiles, finishedFiles, 0);
              })
      );
    }
    Promise.all(promises)
        .then(function () {
          updateZipProgress(files.length, files.length, files.length, 0);
          appendZipLog("Compressing ZIP file...");
          return zip.generateAsync({type: "blob"}, function (metadata) {
            updateZipProgress(
                files.length,
                files.length,
                files.length,
                metadata.percent
            );
          });
        })
        .then(function (blob) {
          appendZipLog("Downloading ZIP file...");
          saveAs(blob, "download-" + meta.model + ".zip");
          appendZipLog("Done");
          updateZipProgress(files.length, files.length, files.length, 100);
          $("#zip-progress").css("width", 100 + "%");
          setTimeout(function () {
            $("#zipModel").modal("hide");
          }, 2000);
        })
        .catch(function (err) {
          console.warn(err);
        });
  })
};

function appendZipLog(message) {
  const el = $("#zip-log");
  el.append(message + "\n");
  el.scrollTop(el.prop("scrollHeight"));
}

function updateZipProgress(total, started, finished, compressing) {
  // Starting a files request up to 40%, Finishing a file request up to 40%, Building Zip up to 10%
  const percent =
    (started / total) * 0.4 +
    (finished / total) * 0.4 +
    (compressing / 100) * 0.1;
  $("#zip-progress").css("width", percent * 100 + "%");
  $("#zip-progress").addClass("progress-bar-striped progress-bar-animated");
}

function bytesToSize(bytes) {
  var sizes = ["Bytes", "KB", "MB", "GB", "TB"];
  if (bytes == 0) return "0 Byte";
  var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
  return Math.round(bytes / Math.pow(1024, i), 2) + " " + sizes[i];
}

module.exports = {
  init : init
};