const path = require('path');
const webpack = require('webpack');

function jquery() {
    // See https://stackoverflow.com/questions/28969861/managing-jquery-plugin-dependency-in-webpack
    return new webpack.ProvidePlugin({
        $: "jquery",
        jQuery: "jquery"
    });
}

module.exports = {
    mode: "production", // "production" | "development" | "none"
    entry: "./src/main/js/index.js",
    output: {
        path: path.resolve(__dirname, "src/main/assets/assets/js"),
    },
    plugins: [
        jquery()
    ]
}