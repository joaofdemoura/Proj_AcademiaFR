'use client';
import {useEffect,useState} from 'react';
import Dashboard from './dashboard';
import Login from './login-screen';
import type {PanelUser} from '@/lib/panel-api';

/** Mostra o login ou o painel conforme a sessão na API. */
export default function PanelApp(){
 const [user,setUser]=useState<PanelUser|null>(null),[state,setState]=useState<'loading'|'login'|'authenticated'>('loading');
 useEffect(()=>{fetch('/api/auth/me').then(async r=>{if(r.ok){setUser(await r.json());setState('authenticated')}else setState('login')}).catch(()=>setState('login'))},[]);
 if(state==='loading')return <div className="loading">Carregando…</div>;
 return state==='authenticated'&&user?<Dashboard currentUser={user}/>:<Login/>;
}
