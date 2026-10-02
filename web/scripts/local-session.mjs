import {randomBytes,scryptSync,timingSafeEqual,createHash} from 'node:crypto';
import {AsyncLocalStorage} from 'node:async_hooks';
import {existsSync,readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import path from 'node:path';
const directory=path.resolve(process.env.GYM_LOCAL_DATA||'.local');mkdirSync(directory,{recursive:true});
const accountPath=path.join(directory,'accounts.json');
const oldPath=path.join(directory,'admin.json');
if(!existsSync(accountPath)&&existsSync(oldPath)){const old=JSON.parse(readFileSync(oldPath,'utf8'));writeFileSync(accountPath,JSON.stringify([{...old,role:'global_admin',unit:null,displayName:'Administrador geral'}]),{mode:0o600})}
const accounts=()=>existsSync(accountPath)?JSON.parse(readFileSync(accountPath,'utf8')):[];
export function createAccount(username,password,unit=null){const all=accounts();if(all.some(a=>a.username===username))throw new Error('Conta já existe: '+username);const salt=randomBytes(16).toString('hex');all.push({username,salt,hash:scryptSync(password,salt,64).toString('hex'),role:unit?'unit_admin':'global_admin',unit,displayName:unit?'Admin · '+unit.split(' ')[0]:'Administrador geral'});writeFileSync(accountPath,JSON.stringify(all),{mode:0o600})}
const sessions=new Map();const attempts=new Map();const duration=8*60*60*1000;
const digest=value=>createHash('sha256').update(value).digest('hex');
const publicUser=a=>({userId:a.username,username:a.username,displayName:a.displayName,email:a.username+'@localhost',fullName:a.displayName,role:a.role,unit:a.unit});
export function session(req){const token=(req.headers.cookie||'').split(';').map(s=>s.trim()).find(s=>s.startsWith('gym_session='))?.slice(12);if(!token)return null;const id=digest(token),entry=sessions.get(id);if(!entry)return null;if(entry.until<Date.now()){sessions.delete(id);return null}const account=accounts().find(a=>a.username===entry.username);return account?publicUser(account):null}
export function login(req,username,password){const ip=req.socket.remoteAddress;const now=Date.now();const record=attempts.get(ip);if(record&&record.until>now&&record.count>=5)return {status:429,error:'Muitas tentativas. Aguarde 15 minutos e tente novamente.'};const all=accounts();if(!all.length)return {status:503,error:'As contas ainda não foram configuradas.'};const account=all.find(a=>a.username===username);const actual=scryptSync(password,account?.salt||'unregistered-account',64);if(!account||!timingSafeEqual(actual,Buffer.from(account.hash,'hex'))){attempts.set(ip,{count:record&&record.until>now?record.count+1:1,until:record&&record.until>now?record.until:now+15*60*1000});return {status:401,error:'Usuário ou senha incorretos.'}}attempts.delete(ip);for(const [id,entry] of sessions)if(entry.until<now)sessions.delete(id);const token=randomBytes(32).toString('base64url');sessions.set(digest(token),{username,until:now+duration});return {status:200,cookie:`gym_session=${token}; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800`}}
export function logout(req){const token=(req.headers.cookie||'').split(';').map(s=>s.trim()).find(s=>s.startsWith('gym_session='))?.slice(12);if(token)sessions.delete(digest(token));return 'gym_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0'}
export function ensureAdmin(){for(const [username,unit] of [['admin',null],['admin.prime','Pryme Academia'],['admin.move','Move Academia']]){if(!accounts().some(a=>a.username===username)){const password='Pm!'+randomBytes(9).toString('base64url');createAccount(username,password,unit);console.log('Conta criada | Usuário: '+username+' | Senha: '+password)}}}
const context=new AsyncLocalStorage();
export const runWithUser=(user,callback)=>context.run(user,callback);
export const getLocalUser=()=>context.getStore()||null;
