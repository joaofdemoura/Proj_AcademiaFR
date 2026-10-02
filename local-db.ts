import {DatabaseSync} from 'node:sqlite';
import {mkdirSync} from 'node:fs';
import {resolve} from 'node:path';
const directory=resolve(process.env.GYM_LOCAL_DATA||'.local');
mkdirSync(directory,{recursive:true});
const database=new DatabaseSync(resolve(directory,'gym.sqlite'));
// Isolated local preview schema; production migrations remain owned by Drizzle.
database.exec('CREATE TABLE IF NOT EXISTS records (id TEXT PRIMARY KEY, kind TEXT NOT NULL, payload TEXT NOT NULL)');
class Statement{
 args: (string|number|null)[]=[];
 constructor(public sql:string){}
 bind(...args:(string|number|null)[]){this.args=args;return this}
 async first<T>(){return database.prepare(this.sql).get(...this.args) as T||null}
 async all<T>(){return {results:database.prepare(this.sql).all(...this.args) as T[]}}
 async run(){return database.prepare(this.sql).run(...this.args)}
}
export const env={DB:{prepare:(sql:string)=>new Statement(sql),batch:async(statements:Statement[])=>{database.exec('BEGIN');try{const results=[];for(const s of statements)results.push(await s.run());database.exec('COMMIT');return results}catch(e){database.exec('ROLLBACK');throw e}}}};
