import {createRoot} from 'react-dom/client';
import {useEffect,useState} from 'react';
import Dashboard from '../app/dashboard';
import Login from '../app/login-screen';
function App(){const [user,setUser]=useState<{username:string;displayName:string;role:string;unit:string|null}|null>(null);const [state,setState]=useState('loading');useEffect(()=>{fetch('/api/auth/me').then(async r=>{if(r.ok){setUser(await r.json());setState('authenticated')}else setState('login')}).catch(()=>setState('login'))},[]);if(state==='loading')return <div className="loading">Carregando…</div>;return state==='authenticated'?<Dashboard localMode currentUser={user||undefined}/>:<Login/>}
createRoot(document.getElementById('root')!).render(<App/>);
