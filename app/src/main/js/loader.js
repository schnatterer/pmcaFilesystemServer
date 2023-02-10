const LazyLoad = require('vanilla-lazyload')

function init(base, meta) {

    document.title = "Loading...";
    const days = [];

    fetch(base + "/api/meta.do").then(async (resp) => {
        const el = document.getElementById("title");
        meta = await resp.json();
        el.innerText = `${meta.brand} ${meta.model}`;
        $("#link-logfile").attr("href", base + meta.log);
    });

    // load filter settings from local storage
    if (localStorage.getItem("filetype-filter") != null) {
        const filter = localStorage.getItem("filetype-filter").split(",");
        $(".filter-checkbox").prop("checked", false);
        for (const f of filter) {
            $(".filter-checkbox#" + f).prop("checked", true);
        }
    }

    onFilterChanged(base, days, meta);

    $("#filter").click(function () {
        onFilterChanged(base, days, meta);
    });
}

function loadFiles(base, types, meta, days) {
    document.title = "Loading...";
    $("#download").hide();
    const li = document.getElementById("main-gallery");
    li.innerHTML = `
      <div class="alert alert-info mx-auto">
        <strong>Loading!</strong> Please wait...
      </div>`;
    fetch(base + "/api/list.do?type=" + types).then(async (resp) => {
        const json = await resp.json();

        if (json.length === 0) {
            li.innerHTML = `
            <div class="alert alert-danger mx-auto">
              <strong>No media!</strong> No media found for your current filter.
            </div>`;
            return;
        }

        li.innerHTML = "";

        days.length = 0;
        let day = null;
        let dayId = "";

        for (const image of json) {
            if (day == null || !isSameDay(image.date, day)) {
                day = new Date(image.date);
                dayId = day.toISOString().substr(0, 10);
                var template = document.createElement("template");
                template.innerHTML = `
              <h4 id="day-${dayId}" class="col-12 clearfix day-title">
                <div class="form-check float-right">
                  <input class="form-check-input position-static checkbox-download-day" type="checkbox" data-day="${dayId}">
                </div>
                ${day.toLocaleDateString()}
              </h4>`;
                li.append(template.content);
                days.push({id: dayId, text: day.toLocaleDateString()});
            }

            var template = document.createElement("template");
            // prettier-ignore
            template.innerHTML = `
            <div class="col-lg-2 col-md-4 col-6">
              <div class="card mb card-link" data-name="${image.name}" data-file="${image.file}" data-size="${image.size}" data-href="${base}${image.file}" data-day="${dayId}">
                <div class="form-check position-absolute">
                  <input class="form-check-input position-static checkbox-download" type="checkbox" id="blankCheckbox" value="option1">
                </div>
                <img data-src="${base}${image.thumbnail}" class="card-img-top img-fluid lazy" alt="${image.name}">
                <div class="card-body">
                  <h5 class="card-title text-info">${image.name}</h5>
                  <p class="card-text">${new Date(image.date).toLocaleString()}</p>
                </div>
              </div>
            </div>
            `;
            li.append(template.content);
        }

        // Days to sidebar
        const dateLi = document.getElementById("date-list");
        dateLi.innerHTML = "";
        var template = document.createElement("template");
        for (const day of days) {
            // prettier-ignore
            template.innerHTML = `<a class="dropdown-item" href="#day-${day.id}">${day.text}</a>`;
            dateLi.append(template.content);
        }

        $("body").scrollspy({target: "#date-list"});

        var lazyLoadInstance = new LazyLoad({});

        document.title = `${json.length} images - ${meta.brand} ${meta.model}`;
    });
}

function onFilterChanged(base, days, meta) {
    let filter = "";
    $(".filter-checkbox:checked").each(function (el) {
        filter += $(this).attr("id") + ",";
    });
    while (filter.endsWith(",")) {
        filter = filter.substr(0, filter.length - 1);
    }
    // save filter settings to local storage
    localStorage.setItem("filetype-filter", filter);
    loadFiles(base, filter, meta, days);
}

function isSameDay(day, date) {
    return (
        new Date(day).toISOString().substr(0, 10) ===
        date.toISOString().substr(0, 10)
    );
}

module.exports = {
    init : init
};

