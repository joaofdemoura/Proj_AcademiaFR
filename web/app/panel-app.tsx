'use client';
import {useEffect,useState} from 'react';
import Dashboard from './dashboard';
import Login from './login-screen';
import type {PanelUser} from '@/lib/panel-api';

export default function PanelApp(){
 const [user,setUser]=useState<PanelUser|null>(null),[loading,setLoading]=useState(true);
 async function refreshSession(){
  try{const r=await fetch('/api/auth/me');setUser(r.ok?await r.json():null)}
  catch{setUser(null)}finally{setLoading(false)}
 }
 useEffect(()=>{void refreshSession();const restore=()=>void refreshSession();window.addEventListener('pageshow',restore);return()=>window.removeEventListener('pageshow',restore)},[]);
 if(loading)return <div className="loading">Carregando…</div>;
 return user?<Dashboard currentUser={user} onLogout={()=>{setUser(null);window.history.replaceState(null,'','/login')}}/>:<Login onAuthenticated={async()=>{await refreshSession();window.history.replaceState(null,'','/')}}/>;
}
