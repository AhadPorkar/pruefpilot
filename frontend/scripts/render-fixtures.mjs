/** Deterministic, network-free render fixtures for visual QA. Not a live application substitute. */
import {mkdir,readFile,writeFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createDemoState} from '../dist/app/seed.js';
import {shell,pageView,loginView} from '../dist/app/views.js';
import {translator} from '../dist/app/i18n.js';
import {dialogView} from '../dist/app/dialogs.js';
import {qrSvg} from '../dist/app/qr.js';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const output=process.env.QA_FIXTURES??path.join(root,'test-results/render-fixtures');await mkdir(output,{recursive:true});
const css=await readFile(path.join(root,'public/styles.css'),'utf8');const state=createDemoState('2026-10-23');
for(const language of ['en','de'])for(const theme of ['light','dark'])for(const route of ['overview','assets','asset/'+state.assets[0].id,'inspections','schedules','actions','reports','templates','team','audit','help','login']){
 const a=state.assets[0],ids=new Set(state.inspections.filter(i=>i.assetId===a.id).map(i=>i.id));
 const passport={asset:a,inspections:state.inspections.filter(i=>ids.has(i.id)),answers:state.answers.filter(i=>ids.has(i.inspectionId)),findings:state.findings.filter(i=>i.assetId===a.id),evidence:state.evidence.filter(i=>i.assetId===a.id),audit:state.audit,page:0,hasMore:false};
 const context={state,t:translator(language),language,route,site:'',query:'',category:'',layout:'grid',preview:true,passport};
 const content=route==='login'?loginView(context.t,language):shell(context,pageView(context));
 const html=`<!doctype html><html lang="${language}" data-theme="${theme}"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>${css}</style><body><pruef-pilot>${content}</pruef-pilot></body></html>`;
 await writeFile(path.join(output,`${route.split('/')[0]}-${language}-${theme}.html`),html);
}
const c={state,t:translator('en'),language:'en',route:'inspections',site:'',query:'',category:'',layout:'grid',preview:true};
for(const kind of ['new-site','new-asset','new-template','new-schedule','new-inspection','new-member','inspection','finding','template','password']){
 const id=kind==='inspection'?state.inspections[4].id:kind==='finding'?state.findings[0].id:kind==='template'?state.templates[0].id:'';
 const view=dialogView(c,{kind,id});
 await writeFile(path.join(output,`${kind}-dialog.html`),`<html lang="en"><meta charset="utf-8"><style>${css}dialog{position:relative;max-height:none;margin:25px auto}</style><body><dialog open class="modal ${view.wide?'modal-wide':''}"><header class="modal-header"><h2>${view.title}</h2></header><div class="modal-content">${view.body}</div></dialog></body></html>`);
}
await writeFile(path.join(output,'qr.html'),`<html><body style="margin:0">${qrSvg('https://pilot.example.com/#/asset/'+state.assets[0].id,'PruefPilot asset')}</body></html>`);
console.log(`Render fixtures: ${output}`);
