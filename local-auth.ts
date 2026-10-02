// Shared request context is populated only after server-side session validation.
import {getLocalUser} from '../scripts/local-session.mjs';
export async function getChatGPTUser(){return getLocalUser()}
