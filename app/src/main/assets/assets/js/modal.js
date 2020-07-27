$(document).on("click", ".card-link", function (event) {
    if (event.target.tagName === "INPUT") {
        return;
    }

    if ($(".checkbox-download:checked").length == 0 || $(event.target).parents('.card-body').length > 0) {
        // If we haven't checked a download box or click the card-body, we open the modal
        $("#infoModel").data("file", $(this).data("file"));
        $("#infoModel").data("href", $(this).data("href"));
        $("#infoModel").modal("show");
    } else {
        // If a download box is checked and we click on the image, we check to download box instead of showing the modal
        const el = $(this).find(".form-check-input");
        el.prop("checked", !el.prop("checked"));
        el.trigger("change");
    }
});

$("#infoModel").on("show.bs.modal", function (event) {
    const modal = $(this);

    modal.find(".modal-image").attr("src", "");
    modal.find(".model-link").attr("href", "");
    modal.find(".model-link").attr("download", "");
    modal.find(".modal-title").text("Loading...");
    modal.find(".modal-detail").text("");

    const file = $(this).data("file");
    const link = $(this).data("href");
    fetch(base + "/api/exif.do?f=" + file).then(async (resp) => {
        const json = await resp.json();

        modal.find(".modal-image").attr("src", link);
        modal.find(".model-link").attr("href", link);
        modal.find(".model-link").attr("download", json.Name);
        modal.find(".modal-title").text(json.Name);

        // prettier-ignore
        let detail = `
        <p><i class="icon-camera"></i> ${json.Model}</p>
        <p><i class="icon-camera"></i> ${json.LensSpecification}</p>
        <p><i class="icon-screen"></i> ${new Date(json.LastModified).toLocaleString()}</p>
        <p class="mb-3"><i class="icon-storage"></i> ${Math.round((json.ImageWidth * json.ImageLength) / 1000000)} MP ${json.ImageWidth} × ${json.ImageLength}</p>
        <p><i class="icon-aperture"></i> ${json.FNumber}</p>
        <p><i class="icon-speed"></i> ${json.ExposureTime}</p>
        <p><i class="icon-focal"></i> ${json.FocalLength}&nbsp;mm</p>
        <p><i class="icon-iso"></i> ${json.ISOSpeedRatings}</p>
        `;
        modal.find(".modal-detail").html(detail);
    });
});