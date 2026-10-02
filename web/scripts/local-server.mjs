import http from 'node:http';
import {readFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {handleGym,handleLogin,handleLogout,handleMe} from '../local-build/api.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const port=Number(process.env.PORT||5173);
const origin=`http://127.0.0.1:${port}`;
const html='<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Pryme & Move | Gestão</title><link rel="icon" href="/favicon.svg"><link rel="stylesheet" href="/style.css"></head><body><div id="root"></div><script type="module" src="/app.js"></script></body></html>';
const routes={'/api/auth/login':handleLogin,'/api/auth/me':handleMe,'/api/auth/logout':handleLogout,'/api/gym':handleGym};
http.createServer(async(req,res)=>{try{
 if(req.headers.host!==`127.0.0.1:${port}`){res.writeHead(403);res.end('Acesso local somente.');return}
 const url=new URL(req.url,origin);
 res.setHeader('Cache-Control','no-store');
 const handler=routes[url.pathname];
 if(handler){
  let body='';if(req.method==='POST')for await(const chunk of req){body+=chunk;if(body.length>200000){res.writeHead(413);res.end();return}}
  const request=new Request(url,{method:req.method,headers:new Headers(Object.entries(req.headers).filter(([,v])=>typeof v==='string')),body:req.method==='POST'?body:undefined});
  const response=await handler(request);res.writeHead(response.status,Object.fromEntries(response.headers));res.end(await response.text());return;
 }
 if(url.pathname.startsWith('/api/')){res.writeHead(404,{'content-type':'application/json'});res.end(JSON.stringify({error:'Não encontrado.'}));return}
 if(url.pathname==='/'||url.pathname==='/login'){res.writeHead(200,{'content-type':'text/html; charset=utf-8'});res.end(html);return}
 const asset=url.pathname==='/app.js'?path.join(root,'local-build/app.js'):url.pathname==='/style.css'?path.join(root,'app/globals.css'):path.resolve(root,'public','.'+url.pathname);
 if(!asset.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 const mime={'.js':'application/javascript','.css':'text/css','.svg':'image/svg+xml','.png':'image/png','.woff2':'font/woff2'};
 res.writeHead(200,{'content-type':mime[path.extname(asset)]||'application/octet-stream'});res.end(await readFile(asset));
 }catch(e){if(!res.headersSent)res.writeHead(500);res.end('Não foi possível carregar.');console.error(e.message)}}).listen(port,'127.0.0.1',()=>console.log(`Local: ${origin}\nAPI: ${process.env.LARAVEL_API_URL||'http://academia-api.test/api'}`));
