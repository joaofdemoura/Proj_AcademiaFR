import type { Metadata } from 'next';
import './globals.css';
export const metadata:Metadata={title:'Pryme & Move | Gestão',description:'Alunos, planos e treinos das suas academias em um só lugar.',icons:{icon:'/favicon.svg'}};
export default function Layout({children}:{children:React.ReactNode}){return <html lang="pt-BR"><body>{children}</body></html>}
