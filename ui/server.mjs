import fs from 'node:fs'
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const port = Number(process.env.UI_PORT || 9000)
const agentApi = new URL(process.env.AGENT_API_URL || 'http://127.0.0.1:18080')
const businessApi = new URL(process.env.BUSINESS_API_URL || 'http://127.0.0.1:18081')
const apiTargets = {
  '/agent-api/': { hostname: agentApi.hostname, port: Number(agentApi.port || 80) },
  '/business-api/': { hostname: businessApi.hostname, port: Number(businessApi.port || 80) }
}
const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml'
}

function sendJson(response, status, body) {
  response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  response.end(JSON.stringify(body))
}

function serveFile(response, baseDir, relativePath) {
  const target = path.resolve(baseDir, '.' + relativePath)
  if (!target.startsWith(path.resolve(baseDir) + path.sep)) {
    sendJson(response, 403, { message: 'Forbidden' })
    return
  }
  fs.stat(target, (error, stat) => {
    if (error || !stat.isFile()) {
      sendJson(response, 404, { message: 'Not found' })
      return
    }
    response.writeHead(200, {
      'Content-Type': mimeTypes[path.extname(target).toLowerCase()] || 'application/octet-stream',
      'Cache-Control': 'no-cache'
    })
    fs.createReadStream(target).pipe(response)
  })
}

function serveApp(response, folder) {
  serveFile(response, path.join(currentDir, folder), '/index.html')
}

function serveStatic(response, folder, suffix) {
  const relative = suffix === '/' || suffix === '' ? '/index.html' : suffix
  serveFile(response, path.join(currentDir, folder), relative)
}

function proxy(request, response, prefix, target) {
  const targetPath = request.url.slice(prefix.length - 1) || '/'
  const headers = { ...request.headers, host: target.hostname + ':' + target.port }
  const upstream = http.request({
    hostname: target.hostname,
    port: target.port,
    path: targetPath,
    method: request.method,
    headers
  }, upstreamResponse => {
    response.writeHead(upstreamResponse.statusCode || 502, upstreamResponse.headers)
    upstreamResponse.pipe(response)
  })
  upstream.on('error', error => {
    sendJson(response, 502, { message: 'API unavailable', detail: error.message })
  })
  request.pipe(upstream)
}

const server = http.createServer((request, response) => {
  const pathname = new URL(request.url, 'http://localhost').pathname

  if (pathname === '/') return serveFile(response, currentDir, '/index.html')
  if (pathname === '/agent') {
    response.writeHead(302, { Location: '/agent/' })
    return response.end()
  }
  if (pathname === '/business') {
    response.writeHead(302, { Location: '/business/' })
    return response.end()
  }
  if (pathname === '/agent/' || pathname.startsWith('/agent/')) {
    return serveStatic(response, 'agent-console', pathname.slice('/agent'.length))
  }
  if (pathname === '/business/' || pathname.startsWith('/business/')) {
    return serveStatic(response, 'business-console', pathname.slice('/business'.length))
  }
  for (const [prefix, target] of Object.entries(apiTargets)) {
    if (pathname.startsWith(prefix)) return proxy(request, response, prefix, target)
  }
  sendJson(response, 404, { message: 'Not found' })
})

server.listen(port, () => {
  console.log(`UI server: http://localhost:${port}`)
})
