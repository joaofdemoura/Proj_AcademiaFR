import {test} from 'node:test';
import assert from 'node:assert/strict';
import {handleLogin,handleLogout,toPanelUser} from '../lib/panel-api.ts';

const profile=(role,status='active')=>({name:'Teste',username:'teste',email:'teste@example.com',is_global_admin:false,gyms:[{name:'Pryme Academia',status,roles:[role]}]});
test('perfis: personal e admin ficam na unidade; aluno e vínculo inativo não entram',()=>{
 assert.equal(toPanelUser(profile('instructor')).role,'personal');
 assert.equal(toPanelUser(profile('instructor')).unit,'Pryme Academia');
 assert.equal(toPanelUser(profile('admin')).role,'unit_admin');
 assert.equal(toPanelUser(profile('student')),null);
 assert.equal(toPanelUser(profile('instructor','inactive')),null);
 assert.equal(toPanelUser({...profile('admin'),is_global_admin:true}).unit,null);
});
test('login do personal cria cookie protegido e logout o expira',async t=>{
 t.mock.method(globalThis,'fetch',async url=>new Response(JSON.stringify(url.endsWith('/login')?{token:'test-token',user:profile('instructor')}:{ok:true}),{status:200}));
 const login=await handleLogin(new Request('http://localhost/api/auth/login',{method:'POST',headers:{origin:'http://localhost'},body:JSON.stringify({username:'teste',password:'teste'})}));
 assert.equal(login.status,200);
 assert.match(login.headers.get('set-cookie'),/HttpOnly; SameSite=Strict/);
 const logout=await handleLogout(new Request('http://localhost/api/auth/logout',{method:'POST',headers:{origin:'http://localhost',cookie:'gym_token=test-token'}}));
 assert.equal(logout.status,200);
 assert.match(logout.headers.get('set-cookie'),/Max-Age=0/);
});
test('aluno não recebe sessão web; chamadas de outra origem são recusadas',async t=>{
 t.mock.method(globalThis,'fetch',async url=>new Response(JSON.stringify(url.endsWith('/login')?{token:'test-token',user:profile('student')}:{ok:true}),{status:200}));
 const response=await handleLogin(new Request('http://localhost/api/auth/login',{method:'POST',body:JSON.stringify({username:'teste',password:'teste'})}));
 assert.equal(response.status,403);
 assert.equal(response.headers.get('set-cookie'),null);
 assert.equal((await handleLogout(new Request('http://localhost/api/auth/logout',{method:'POST',headers:{origin:'http://unrelated.invalid'}}))).status,403);
});
