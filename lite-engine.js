function LiteEngine(root,config){
	root = require('path').resolve(root || './')
	root = root.replace(/[\\\/]*$/,'/');
	this.root = root;
	this.templateMap = {};
	this.renderTask = {};
	var thiz = this;
	var filter = config&&config.filter;
	try{
		//throw new Error();
		this.compiler = require('child_process').fork(__dirname + '/process.js',['-root',root,'-filter',filter]);
		this.compiler.on('message', function(result){
			thiz.onChange(result.path,result.code,result.config,result.prefix)
		}); 
		
	}catch(e){
		if(this.compiler == null){
			var thiz = this;
			var setupCompiler = require('./process.js').setupCompiler;
			var compiler = setupCompiler(root,function(result){
					var action = result.action;
					if(action == 'remove' || action == 'add' || action=='error'){
						thiz.onChange(result.path,result.code,result.config,result.prefix)
					}
				},filter);
			this.compiler = {
				send:compiler
			}
			
		}
	}
}
LiteEngine.prototype.requestCompile = function(path){
	this.compiler.send(path);
}
LiteEngine.prototype.onChange = function(path,code,config,prefix) {
	if(code){
		var tpl = new Template(code,config,prefix||'');
		if(config.error == null){//发生错误的页面每次都需要重建？？
			this.templateMap[path] = tpl; 
		}
		var task = this.renderTask[path];
		if(task){
			delete this.renderTask[path];
			for(var i=0;i<task.length;i++){
				var args = task[i];
				args[0] = tpl;
				doRender.apply(null,args)
			}
		}
	}else{//clear cache
		delete this.templateMap[path];
		console.info('clear template cache:' ,path);
	}
}
LiteEngine.prototype.render=function(path,model,req,response){
    var cookie = String(req.headers.cookie);
    var debug = cookie.replace(/(?:^|&[\s\S]*;\s*)LITE_DEBUG=(\w+)[\s\S]*$/,'$1');
    debug = debug == cookie?false:debug;
	if(debug=='model'){
    	response.end(JSON.stringify(model));
    }else if(debug=='source'){
    	require('fs').readFile(require('path').resolve(this.root ,path.replace(/^[\\\/]/,'')), "binary", function(err, file) {    
        	if(err) {
            	response.writeHead(404, {"Content-Type": "text/plain"});   
            	response.end(err + "\n");    
        	}else{
        		response.writeHead(200, {"Content-Type": 'text/plain;charset=utf8'}); 
         		response.end(file, "binary"); 
        	}    
    	});
   	}else{
		var tpl = this.templateMap[path];
		if(tpl){
			doRender(tpl,model,response);
		}else{
			(this.renderTask[path] || (this.renderTask[path] =[])).push([path,model,response]);
			this.requestCompile(path);
		}
	}
}
function doRender(tpl,model,response){
    response.writeHead(200, {"Content-Type": tpl.contentType});   
	//response.write(tpl.prefix,'utf-8');

	if(typeof model == 'function'){
		//TODO,需要引擎级别实现异步,这里知识兼容一下接口
		renderAsync(tpl,model,response)
	}else{
		try{
			tpl.render(model,response);
		}catch(e){
			var rtv = '<pre>'+require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
			response.end(rtv);
			throw e;
		}
	}
}
function renderAsync(tpl,modelLoader,response){
	modelLoader(function(model){
		try{
			tpl.render(model,response);
		}catch(e){
			rtv = require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
			response.end(rtv);
		}
	});
}
function Template(code,config,prefix){
 	//console.log(code)
 	
	try{
    	this.impl = eval('['+code+'][0]');
    }catch(e){
    	//console.error(config.path,require('util').inspect(e,true)+'\n\n'+(e.message +e.stack));
    	this.impl = function(){throw e;};
    }
    //console.log(this.impl .toString());
    this.config = config;
    this.contentType = config.contentType;
    this.encoding = config.encoding;
    this.prefix = prefix ;
}
Template.prototype.render = function(context,response){
	try{
		this.impl.call(null,context,wrapResponse(response));
	}catch(e){
		console.warn(this.impl+'');

		var rtv = require('util').inspect(e,true)+'\n\n'+(e.message +e.stack);
		response.end(rtv);
		throw e;
	}
}
function wrapResponse(resp){
	return {
		lazyList:[],
		push:function(){
			for(var len = arguments.length, i = 0;i<len;i++){
				//console.log(arguments[i])
				resp.write(arguments[i]);
			}
		},
		wait:modelWait,
		lazy:function(g){
			this.lazyList.push(g);
		},
		join:function(){
			if(!doMutiLazyLoad(this.lazyList,resp)){
				resp.end();
			}
		}
	}
}
function* modelWait(){
	var i = arguments.length;
	while(i--){
		//resp.flush();
		if (arguments[i] instanceof Promise) {
			yield arguments[i]
		};
	}
}

function doMutiLazyLoad(lazyList,resp){
	var len = lazyList.length;
	var dec = len;
	for(var i = 0;i<len;i++){
		startModule(lazyList[i],[]);
	}

	//console.log('lazy module:',len,lazyList)
	function startModule(g,r){
		var id = g.name;
		r.wait = modelWait;
		g = g(r);
		function next(){
			var n = g.next();
			//console.log('do next:',n)
			if(n.done){
				//console.log('done');
				var rtv = r.join('');
				//console.log('#$%$###### item',rtv)
				resp.write('<script>moduleLoaded("'+id+'",'+JSON.stringify(rtv)+')</script>')
				if(--dec == 0){
					//console.log('#$%$######end')
					resp.end();
				}
				return rtv;
			}else{
				 n.value.then(next);
				 //console.log('is promise',n.value)
			}
		}

		//console.log('lazy module:',id)
		next();
	}
	return len;

}
exports.LiteEngine = LiteEngine;
exports.Template = Template;