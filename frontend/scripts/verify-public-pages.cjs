/** Checks static rendering and navigation destinations for the public CivicConnect pages. */
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const ts = require(path.join(root, 'node_modules/typescript'));
for (const suffix of ['.tsx', '.ts']) {
  require.extensions[suffix] = (mod, filename) => {
    const source = fs.readFileSync(filename, 'utf8');
    const compiled = ts.transpileModule(source, { compilerOptions: {
      target: ts.ScriptTarget.ES2022,
      module: ts.ModuleKind.CommonJS,
      jsx: ts.JsxEmit.ReactJSX,
      esModuleInterop: true,
    }}).outputText;
    mod._compile(compiled, filename);
  };
}
require.extensions['.css'] = (mod) => { mod.exports = {}; };
const React = require(path.join(root, 'node_modules/react'));
const {renderToStaticMarkup} = require(path.join(root, 'node_modules/react-dom/server'));
global.window = { location: { pathname: '/' } };
const App = require(path.join(root, 'src/App.tsx')).default;
const requiredByPage = {
  '/': ['A stronger', 'Welcome back', 'Continue with Google', 'Forgot password?', 'Create account', 'Submit requests'],
  '/how-it-works': ['How CivicConnect works', 'A team reviews it', 'Follow the progress', 'See the resolution'],
  '/services': ['Services and support categories', 'Facility Fault', 'Damaged Equipment', 'IT Support', 'Lost Property'],
  '/about': ['Better service starts with connection', 'Clarity', 'Accountability', 'Visibility'],
  '/forgot-password': ['Reset your password.', 'Request password reset', 'Back to sign in'],
};
let errors = 0;
for (const [route, snippets] of Object.entries(requiredByPage)) {
  global.window.location.pathname = route;
  const html = renderToStaticMarkup(React.createElement(App));
  const missing = snippets.filter(snippet => !html.includes(snippet));
  for (const link of ['/', '/how-it-works', '/services', '/about']) {
    if (!html.includes(`href="${link}"`)) missing.push(`Missing ${link} navigation link`);
  }
  if (missing.length) {console.error('FAIL', route, missing); errors++;}
  else console.log('PASS', route, '- complete page markup and navigation present');
}
if (errors) process.exitCode = 1;
