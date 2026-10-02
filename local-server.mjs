import http from 'node:http';
import {session,login,logout,ensureAdmin,runWithUser} from './local-session.mjs';
import {readFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {GET,POST} from '../local-build/api.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
ensureAdmin();
const port=Number(process.env.PORT||5173);
const origin=`http://127.0.0.1:${port}`;
const html='<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Pryme & Move | Gestão</title><link rel="icon" href="/favicon.svg"><link rel="stylesheet" href="/style.css"></head><body><div id="root"></div><script type="module" src="/app.js"></script></body></html>';
http.createServer(async(req,res)=>{try{
 if(req.headers.host!==`127.0.0.1:${port}`){res.writeHead(403);res.end('Acesso local somente.');return}
 const url=new URL(req.url,origin);
 res.setHeader('Cache-Control','no-store');
 if(url.pathname.startsWith('/api/auth/')){
  const send=(status,data,cookie)=>{if(cookie)res.setHeader('Set-Cookie',cookie);res.writeHead(status,{'content-type':'application/json'});res.end(JSON.stringify(data))};
  if(url.pathname==='/api/auth/me'&&req.method==='GET'){send(session(req)?200:401,session(req)?session(req):{error:'Entre na sua conta.'});return}
  if(req.method!=='POST'){send(405,{error:'Método não permitido.'});return}
  if(req.headers.origin!==origin){send(403,{error:'Origem inválida.'});return}
  if(url.pathname==='/api/auth/logout'){send(200,{ok:true},logout(req));return}
  if(url.pathname!=='/api/auth/login'){send(404,{error:'Não encontrado.'});return}
  let raw='';for await(const chunk of req){raw+=chunk;if(raw.length>4096){send(413,{error:'Dados inválidos.'});return}}
  let body;try{body=JSON.parse(raw)}catch{send(400,{error:'Dados inválidos.'});return}
  if(typeof body.username!=='string'||typeof body.password!=='string'||body.password.length>256){send(400,{error:'Informe usuário e senha.'});return}
  const result=login(req,body.username.trim(),body.password);send(result.status,result.error?{error:result.error}:{ok:true},result.cookie);return
 }
 if(url.pathname.startsWith('/api/')&&!session(req)){res.writeHead(401,{'content-type':'application/json'});res.end(JSON.stringify({error:'Entre na sua conta para continuar.'}));return}
 if(url.pathname==='/'&&!session(req)){res.writeHead(302,{Location:'/login'});res.end();return}
 if(url.pathname==='/login'&&session(req)){res.writeHead(302,{Location:'/'});res.end();return}
 if(url.pathname==='/api/gym'){
  if(!['GET','POST'].includes(req.method)){res.writeHead(405);res.end();return}
  if(req.method==='POST'&&req.headers.origin!==origin){res.writeHead(403);res.end('Origem inválida');return}
  let body='';for await(const chunk of req){body+=chunk;if(body.length>200000){res.writeHead(413);res.end();return}}
  const request=new Request(url,{method:req.method,headers:new Headers(Object.entries(req.headers).filter(([,v])=>typeof v==='string')),body:req.method==='POST'?body:undefined});
  const response=await runWithUser(session(req),()=>req.method==='POST'?POST(request):GET());res.writeHead(response.status,Object.fromEntries(response.headers));res.end(await response.text());return;
 }
 if(url.pathname==='/'||url.pathname==='/login'){res.writeHead(200,{'content-type':'text/html; charset=utf-8'});res.end(html);return}
 const asset=url.pathname==='/app.js'?path.join(root,'local-build/app.js'):url.pathname==='/style.css'?path.join(root,'app/globals.css'):path.resolve(root,'public','.'+url.pathname);
 if(!asset.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 const mime={'.js':'application/javascript','.css':'text/css','.svg':'image/svg+xml','.png':'image/png','.woff2':'font/woff2'};
 res.writeHead(200,{'content-type':mime[path.extname(asset)]||'application/octet-stream'});res.end(await readFile(asset));
 }catch(e){if(!res.headersSent)res.writeHead(500);res.end('Não foi possível carregar.');console.error(e.message)}}).listen(port,'127.0.0.1',()=>console.log(`Local: ${origin}\nBanco SQLite persistente: .local/gym.sqlite`));
