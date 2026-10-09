const {test} = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
function load(file, dependencies = {}) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8')
  const code = ts.transpileModule(source, {compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: false}}).outputText
  const module = {exports: {}}
  vm.runInNewContext(code, {exports: module.exports, module, require: name => {if (name in dependencies) return dependencies[name]; throw new Error(`Unexpected import: ${name}`)}, window: {dispatchEvent() {}, addEventListener() {}}, Event, FormData, Error})
  return module.exports
}
const workflow = load('src/pages/institutions/workflow.ts')
test('reload derives safe wizard steps from authoritative status', () => {
  for (const [status, request, expected] of [['CREATED',5,1],['UPLOADED',5,2],['UPLOADED',1,1],['READY_FOR_REVIEW',5,5],['VALIDATION_COMPLETED',2,3],['PROCESSING',3,6],['IMPORTING',5,6],['COMPLETED',1,7]]) assert.equal(workflow.resumeStep({status}, request), expected)
})
test('duplicate resolution counts incoming records, not candidate matches', () => {
  const rows = [{importRecordId:1,resolution:'PENDING'},{importRecordId:1,resolution:'MERGED'},{importRecordId:2,resolution:'PENDING'},{importRecordId:2,resolution:'PENDING'},{importRecordId:3,resolution:'KEPT_SEPARATE'}]
  assert.equal(workflow.unresolvedRecords(rows), 1)
  assert.equal(workflow.unresolvedRecords([]), 0)
})
test('API adapter preserves source keys, sends real endpoint params and snake_case create payload', async () => {
  const calls = []
  const httpClient = {get: async (...args) => {calls.push(args); return {data:{content:[{short_name:'ABC',raw_data:{original_header:'keep'}}],total_elements:1,total_pages:1,number:0,size:20}}}, post: async (...args) => {calls.push(args); return {data:{id:4,source_id:8}}}}
  const {institutionAdminService: api} = load('src/services/institutionAdminService.ts', {'../api/httpClient': {httpClient}})
  const result = await api.list('2','abc',3)
  assert.equal(result.content[0].shortName,'ABC'); assert.equal(result.content[0].rawData.original_header,'keep'); assert.equal(result.totalElements,1)
  assert.equal(calls[0][0],'/admin/institutions/search'); assert.equal(calls[0][1].params.page,3); assert.equal(calls[0][1].params.countryId,'2')
  await api.create(2,8,'2026.1','FULL'); assert.equal(calls[1][1].country_id,2); assert.equal(calls[1][1].source_id,8); assert.equal(calls[1][1].import_type,'FULL')
  await api.records(4,0,'ERROR'); assert.equal(calls[2][1].params.classification,'ERROR')
})
test('unsafe server details never reach the UI error message', () => {
  const {institutionError} = load('src/services/institutionAdminService.ts', {'../api/httpClient': {httpClient:{}}})
  assert.equal(institutionError(null),'Unable to complete this request. Please try again.')
  assert.equal(institutionError(new Error('java.lang.NullPointerException at service.Import.apply (HTTP 500)')), 'The server could not complete this request. Please try again or contact your administrator.')
  assert.equal(institutionError(new Error('Denied (HTTP 403)')), 'You do not have permission to perform this action.')
  assert.equal(institutionError(new Error('Network Error')), 'The server could not be reached. Refresh the import status before retrying an action.')
})

test('read-only and mutation endpoints match the backend contract', async () => {
  const calls = [], httpClient = {get: async (url, options) => {calls.push({method:'GET',url,options}); return {data:url.includes('history') ? {content:[],totalElements:0,totalPages:0,number:0,size:20} : []}}, post: async (url, body) => {calls.push({method:'POST',url,body}); return {data:{}}}}
  const {institutionAdminService: api} = load('src/services/institutionAdminService.ts', {'../api/httpClient': {httpClient}})
  await api.sources('1'); await api.fields(); await api.mappings(8); await api.import(4); await api.summary(4); await api.record(4,7); await api.duplicates(4); await api.resolve(4,9,'merge'); await api.resolve(4,9,'keep-separate'); await api.apply(4); await api.cancel(4); await api.history(2)
  assert.equal(calls[0].options.params.countryId,'1')
  assert.equal(calls[2].options.params.sourceId,8)
  assert.equal(calls[5].url,'/admin/institution-imports/4/records/7')
  assert.equal(calls[7].url,'/admin/institution-imports/4/duplicates/9/merge')
  assert.equal(calls[8].url,'/admin/institution-imports/4/duplicates/9/keep-separate')
  assert.equal(calls[9].method,'POST'); assert.equal(calls[9].url,'/admin/institution-imports/4/apply')
  assert.equal(calls[10].url,'/admin/institution-imports/4/cancel')
  assert.equal(calls[11].options.params.page,2)
})

test('review blocks apply for errors, unresolved duplicates, permission denial and active requests', () => {
  const React = require('react'), {renderToStaticMarkup} = require('react-dom/server')
  const noOp = () => null
  const {default: Review} = load('src/pages/institutions/components/wizard/ImportReview.tsx', {
    'react/jsx-runtime': require('react/jsx-runtime'),
    './ValidationSummary': {default:noOp}, './ImportRecordsTable': {default:noOp},
    '../Shared': {Details:noOp, Notice:({children}) => React.createElement('p',null,children)},
  })
  const base = {item:{id:4,version:'2026.1'},summary:{errorCount:0},unresolved:0,ready:true,busy:false,allowed:true,start:noOp,cancel:noOp}
  const disabled = props => /<button[^>]*disabled=""[^>]*>Start Import<\/button>/.test(renderToStaticMarkup(React.createElement(Review,props)))
  assert.equal(disabled(base),false)
  for (const override of [{summary:{errorCount:1}},{unresolved:1},{ready:false},{busy:true},{allowed:false}]) assert.equal(disabled({...base,...override}),true)
})

test('progress does not invent a percentage when server counters are absent', () => {
  const React = require('react'), {renderToStaticMarkup} = require('react-dom/server')
  const {default: Progress} = load('src/pages/institutions/components/wizard/ImportProgress.tsx', {
    'react/jsx-runtime': require('react/jsx-runtime'), './ValidationSummary':{default:()=>null}, '../Shared':{Notice:()=>null},
  })
  const html = renderToStaticMarkup(React.createElement(Progress,{item:{totalRecords:100}}))
  assert.match(html, /<progress[^>]*max="100"/)
  assert.doesNotMatch(html, /<progress[^>]*value=/)
  const counted = renderToStaticMarkup(React.createElement(Progress,{item:{totalRecords:100,processedRecords:42}}))
  assert.match(counted, /value="42"/)
})

test('dashboard uses its explicit endpoint and list accepts nested Spring page metadata', async () => {
  const calls = [], httpClient = {get: async (...args) => {calls.push(args); return {data:args[0].endsWith('/summary') ? {total_institutions:1250,configured_sources:3,countries_with_sources:2,total_imports:4,recent_imports:[]} : {content:[{id:1}],page:{total_elements:1250,total_pages:1250,number:0,size:1}}}}}
  const {institutionAdminService: api, institutionPage} = load('src/services/institutionAdminService.ts', {'../api/httpClient':{httpClient}})
  const dashboard = await api.overview()
  assert.equal(dashboard.totalInstitutions,1250)
  assert.equal(calls[0][0],'/admin/institutions/summary')
  const result = await api.list('', '', 0)
  assert.equal(result.totalElements,1250)
  assert.equal(result.content.length,1)
  assert.equal(calls[1][0],'/admin/institutions')
  assert.throws(() => institutionPage({content:[]}), /invalid pagination/)
  assert.throws(() => institutionPage({error:'database error'}), /invalid list response/)
})

function renderDashboard(responses) {
  const React = require('react'), {renderToStaticMarkup} = require('react-dom/server'), requests = []
  const wrapper = ({children}) => React.createElement('div',null,children)
  const {default: Dashboard} = load('src/pages/institutions/components/InstitutionDashboard.tsx', {
    'react/jsx-runtime':require('react/jsx-runtime'),
    '@tanstack/react-query':{useQuery: options => {requests.push(options); const entry = responses[options.queryKey[0]] || {}; return {isPending:false,isError:false,isFetching:false,error:null,refetch:()=>{},...entry}}},
    'react-router-dom':{Link:({to,children})=>React.createElement('a',{href:to},children)},
    '../../../services/institutionAdminService':{institutionAdminService:{}},
    './Shared':{root:'/master-data/institutions',Badge:({value})=>React.createElement('span',null,value),Notice:wrapper,QueryState:({error})=>error?React.createElement('p',null,error.message):null},
    './formatting':{date:value=>value||'—'},
    './ImportHistoryTable':{default:({rows})=>React.createElement('p',null,`${rows.length} recent imports`)},
    './wizard/ValidationSummary':{default:({summary})=>React.createElement('p',null,`${summary.totalRecords} records validated`)},
  })
  return {html:renderToStaticMarkup(React.createElement(Dashboard)),requests}
}
test('dashboard displays actual totals, source-country coverage and latest import summary', () => {
  const {html,requests} = renderDashboard({
    'institution-overview':{data:{totalInstitutions:1250,configuredSources:3,countriesWithSources:2,totalImports:17,recentImports:[{id:8,status:'READY_FOR_REVIEW',version:'2026.1',source:{sourceName:'AISHE'},country:{name:'India'}}]}},
    'institution-summary':{data:{status:'READY_FOR_REVIEW',summary:{totalRecords:230}}},
  })
  assert.match(html,/1,250/); assert.match(html,/>17</); assert.match(html,/>2</); assert.match(html,/230 records validated/)
  assert.match(html,/\/import\/8/); assert.match(html,/AISHE/)
  assert.equal(requests.find(row=>row.queryKey[0]==='institution-summary').queryKey[1],8)
})
test('empty and unavailable dashboard results are distinguished', () => {
  const empty = renderDashboard({'institution-overview':{data:{totalInstitutions:0,configuredSources:0,countriesWithSources:0,totalImports:0,recentImports:[]}}})
  assert.match(empty.html,/No imports yet/)
  assert.equal(empty.requests.find(row=>row.queryKey[0]==='institution-summary').enabled,false)
  const failed = renderDashboard({'institution-overview':{isError:true,error:new Error('Unavailable')}})
  assert.match(failed.html,/Unavailable/); assert.doesNotMatch(failed.html,/No imports yet/)
})

test('filters and mapping edits use the backend parameter names and JSON shape', async () => {
  const calls = [], httpClient = {get: async (url, options) => {calls.push({url,options}); return {data:{content:[],total_elements:0,total_pages:0,number:0,size:20}}},put: async (url,body,options) => {calls.push({url,body,options}); return {data:[]}}}
  const {institutionAdminService: api} = load('src/services/institutionAdminService.ts', {'../api/httpClient':{httpClient}})
  await api.list('3','abc',1,{institutionType:'UNIVERSITY',verificationStatus:'PENDING',active:false})
  assert.equal(calls[0].options.params.active,false)
  assert.equal(calls[0].options.params.institutionType,'UNIVERSITY')
  assert.equal(calls[0].options.params.verificationStatus,'PENDING')
  await api.history(2,{countryId:'3',sourceId:'7',status:'COMPLETED',dateFrom:'2026-01-01',dateTo:'2026-12-31'})
  assert.equal(calls[1].options.params.dateFrom,'2026-01-01'); assert.equal(calls[1].options.params.sourceId,'7')
  await api.records(9,0,'NEW','Alpha')
  assert.equal(calls[2].options.params.q,'Alpha')
  await api.saveMappings(7,[{sourceFieldName:' code ',standardFieldCode:'SOURCE_IDENTIFIER',transformationRule:'TRIM'}])
  assert.equal(calls[3].url,'/admin/institution-source-mappings'); assert.equal(calls[3].options.params.sourceId,7)
  assert.equal(calls[3].body[0].source_field_name,'code'); assert.equal(calls[3].body[0].standard_field_code,'SOURCE_IDENTIFIER')
})
