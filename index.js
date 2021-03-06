/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

/**
 * @param data template source（xml source|| dom）
 * @param config {params:params,liteImpl:'liteImpl'}
 * @public
 */
function parseLite(data,config){
	var path = config&&config.path ||data && data.documentURI;
	var root = config&&config.root || path&&String(path).replace(/[^\/\\]+$/,'');
	var parseContext = new ParseContext(root && new ParseConfig(root));
	path && parseContext.setCurrentURI(path)
	if(typeof data == 'string'){
		data = parseContext.loadXML(data);
	}
	parseContext.parse(data);
	try{
		if(config instanceof Array){
			config = {params:config} 
		}
		var translator = new JSTranslator();
		//translator.liteImpl = "lite_impl"
		var code = translator.translate(parseContext.toList(),config);
		//console.log(code)
		data =  new Function('return '+code).apply();
		data.toString=function(){//_$1 encodeXML
			return code;
		}
		return data;
	}catch(e){
		console.error("translate error",e,code)
		throw e;
	}
}


if(typeof require == 'function'){
exports.parseLite=parseLite;
exports.LiteEngine=require('./lite-engine').LiteEngine;
var ParseConfig=require('./parse/config').ParseConfig;
var JSTranslator=require('./parse/js-translator').JSTranslator;
var ParseContext=require('./parse/parse-context').ParseContext;
}