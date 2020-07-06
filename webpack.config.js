var path = require('path');
const VueLoaderPlugin = require('vue-loader/lib/plugin')

module.exports = {
  mode: 'development',
  entry: {
    main: './src/main/resources/app/javascript/journeyplan.js',
    trammap: './src/main/resources/app/javascript/trammap.js',
    traveltimes: './src/main/resources/app/javascript/traveltimes.js'
  },
  output: {
    path: path.resolve(__dirname, 'src/main/resources/app/dist/'),
    filename: '[name].js',
    publicPath: '/app/dist'
  },
  resolve: {
    alias: {
      vue: 'vue/dist/vue.min.js'
    }
  },
  module:{
    rules:[
      {
            test:/\.css$/,
            use:['style-loader','css-loader']
      },
      {
          test: /\.vue$/,
          loader: 'vue-loader'
      },
      {
          test:/\.png$/,
          loader: 'file-loader',
          options: {
                    name: 'images/[name].[ext]',
                  }
      }
    ]
  },
  plugins: [
      new VueLoaderPlugin()
    ]
};