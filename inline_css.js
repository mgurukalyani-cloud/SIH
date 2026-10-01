const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, 'index.html');
const cssPath = path.join(__dirname, 'style.css');

const html = fs.readFileSync(htmlPath, 'utf8');
const css = fs.readFileSync(cssPath, 'utf8');

if (!html.includes('/* Inlined iTantra Tactical Stylesheet */')) {
  const targetTag = '<link rel="stylesheet" href="style.css">';
  const replacement = targetTag + '\n  <style>\n  /* Inlined iTantra Tactical Stylesheet */\n' + css + '\n  </style>';
  const inlined = html.replace(targetTag, replacement);
  fs.writeFileSync(htmlPath, inlined, 'utf8');
  console.log('Successfully inlined style.css into index.html! New size:', inlined.length);
} else {
  console.log('Already inlined');
}
