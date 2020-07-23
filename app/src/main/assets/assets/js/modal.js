$(document).on("click", ".card-body", function (event) {
    if (event.target.tagName === "INPUT") {
        return;
    }
    $("#infoModel").data("file", $(this).parents('.card-link').data("file"));
    $("#infoModel").data("href", $(this).parents('.card-link').data("href"));
    $("#infoModel").modal("show");
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

        let focalLength = json.FocalLength;
        if (focalLength && focalLength.includes("/")) {
            let t = focalLength.split("/");
            if (t.length === 2) {
                focalLength = parseInt(t[0]) / parseInt(t[1]);
            }
        }

        // prettier-ignore
        let detail = `
        <p><i class="icon-camera"></i> ${json.Model}</p>
        <p><i class="icon-screen"></i> ${new Date(json.LastModified).toLocaleString()}</p>
        <p class="mb-3"><i class="icon-storage"></i> ${Math.round((json.ImageWidth * json.ImageLength) / 1000000)} MP ${json.ImageWidth} × ${json.ImageLength}</p>
        <p><i class="icon-aperture"></i> ƒ/${json.FNumber}</p>
        <p><i class="icon-speed"></i> 1/${Math.floor(1 / json.ExposureTime)}</p>
        <p><i class="icon-focal"></i> ${focalLength}&nbsp;mm</p>
        <p><i class="icon-iso"></i> ${json.ISOSpeedRatings}</p>
        `;
        modal.find(".modal-detail").html(detail);
    });
});