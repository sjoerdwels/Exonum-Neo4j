'use strict';
const HtmlWebpackPlugin = require('html-webpack-plugin');
const path = require('path');
const Dotenv = require('dotenv-webpack');

module.exports = {
  entry: './src/app.js',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'bundle.js'
  },

  devtool: false,
  mode: 'development',
  stats: {
    colors: true,
    reasons: true
  },

  plugins: [
    new HtmlWebpackPlugin({template: 'src/assets/index.html'}),
    new Dotenv()
  ],

  resolve: {
    extensions: ['.webpack.js', '.web.js', '.js', '.jsx']
  },

  module: {
    rules: [
      {
        test: /\.(scss)$/,
        use: [
          {
            // Adds CSS to the DOM by injecting a `<style>` tag
            loader: 'style-loader'
          },
          {
            // Interprets `@import` and `url()` like `import/require()` and will resolve them
            loader: 'css-loader'
          },
          {
            // Loader for webpack to process CSS with PostCSS
            loader: 'postcss-loader',
            options: {
              plugins: function () {
                return [
                  require('autoprefixer')
                ];
              }
            }
          },
          {
            // Loads a SASS/SCSS file and compiles it to CSS
            loader: 'sass-loader'
          }
        ]
      },
      {
        test:  /\.(jpe?g|png|gif|svg)$/i,
        loader: 'file-loader?name=[name].[ext]'
      },
      {
        test: /\.js|\.jsx/,
        exclude: /node_modules|bower_components/,
        loader: 'babel-loader'
      }
    ]
  }
};

