import http from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../dist');
const args=process.argv.slice(2);const value=(key,fallback)=>{const at=args.indexOf(key);return at<0?fallback:args[at+1]??fallback;};
const port=Number(value('--port',process.env.PORT??'4173'));const host=value('--host','127.0.0.1');
const mime={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json','.map':'application/json','.svg':'image/svg+xml','.png':'image/png'};
const server=http.createServer(async(req,res)=>{
  const url=new URL(req.url??'/',`http://${host}:${port}`);
  if(url.pathname.startsWith('/api/')||url.pathname.startsWith('/actuator/')){
    const target=new URL(process.env.API_PROXY??'http://127.0.0.1:8080');
    if(target.protocol!=='http:'){res.writeHead(502).end('Development proxy accepts HTTP localhost backend only.');return;}
    const proxy=http.request({hostname:target.hostname,port:target.port||80,path:req.url,method:req.method,headers:{...req.headers,host:target.host}},response=>{res.writeHead(response.statusCode??502,response.headers);response.pipe(res);});
    proxy.on('error',()=>{if(!res.headersSent)res.writeHead(502,{'Content-Type':'application/json'});res.end('{"code":"networkError"}');});req.pipe(proxy);return;
  }
  try{
    const decoded=decodeURIComponent(url.pathname);const file=path.resolve(root,'.'+decoded);
    if(!file.startsWith(root+path.sep)&&file!==root){res.writeHead(403).end();return;}
    const info=await stat(file);const target=info.isDirectory()?path.join(file,'index.html'):file;
    const bytes=await readFile(target);res.writeHead(200,{'Content-Type':mime[path.extname(target)]??'application/octet-stream','Cache-Control':'no-cache','X-Content-Type-Options':'nosniff','Content-Security-Policy':"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'",'Referrer-Policy':'same-origin'});res.end(bytes);
  }catch{res.writeHead(404,{'Content-Type':'text/plain'});res.end('Not found. Run npm run build first.');}
});
server.listen(port,host,()=>console.log(`PruefPilot: http://${host}:${port}/?demo=1 (explicit fictional preview). API proxy: ${process.env.API_PROXY??'http://127.0.0.1:8080'}`));
process.on('SIGTERM',()=>server.close());process.on('SIGINT',()=>server.close());
