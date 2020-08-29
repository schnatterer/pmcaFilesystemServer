require('bootstrap');
global.$ = require('jquery');
global.fetch = require("node-fetch");
const modal = require('../../main/js/loader');
const fetchMock = require('fetch-mock')

const path = require('path')
const html = require('fs').readFileSync(path.resolve(__dirname, '../../main/assets/index.html')).toString();

let actualMeta = {}
const expectedBase = 'somewhere'
let expectedBrand = 'someBrand';
let expectedModel = 'aModel';
let expectedLog = '/path/log';
let expectedMeta = {
    brand: expectedBrand,
    model: expectedModel,
    log: expectedLog
};

beforeEach(() => {
    document.documentElement.innerHTML = html;
});

afterEach(() => {
        fetchMock.reset()
})

// Testing jquery https://www.phpied.com/jest-jquery-testing-vanilla-app/
describe("loader", () => {

    test('fetches actualMeta on load', async () => {

        mockResponse(expectedMeta, {});

        modal.init(expectedBase, actualMeta)
        expect(document.title).toBe("Loading...");
        
        await fetchMock.flush()
        
        expect(document.getElementById("title").innerText).toBe(`${expectedBrand} ${expectedModel}`);
        expect($("#link-logfile").attr("href")).toEqual(`${expectedBase}${expectedLog}`)
        // TODO shouldn't meta be set?
        // expect(actualMeta).toEqual(expect.objectContaining(expectedMeta))
    });
    
    // TODO test loading files, changing filter, etc.
});

function mockResponse(metaResponse, listResponse) {
    fetchMock.get(`${expectedBase}/api/meta.do`,
        metaResponse);
    fetchMock.get(`${expectedBase}/api/list.do?type=image,video,raw`,
        listResponse)
}