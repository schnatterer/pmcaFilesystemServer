require ('bootstrap');

const base = "";
let meta = {};

require('../../main/js/loader').init(base, meta);
require('../../main/js/modal')(base);
require('../../main/js/download').init(meta)