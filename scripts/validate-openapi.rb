#!/usr/bin/env ruby

require 'yaml'

path = ARGV.fetch(0, 'docs/openapi.yaml')
document = YAML.safe_load(File.read(path))
errors = []

errors << 'openapi must be 3.0.x' unless document['openapi'].to_s.start_with?('3.0.')
errors << 'info.title is required' unless document.dig('info', 'title')
errors << 'info.version is required' unless document.dig('info', 'version')
errors << 'paths must be a non-empty object' unless document['paths'].is_a?(Hash) && !document['paths'].empty?

required_routes = {
  '/api/v1/auctions' => ['post'],
  '/api/v1/auctions/{auctionId}' => %w[get put patch],
  '/api/v1/auctions/{auctionId}/lots' => %w[get post],
  '/api/v1/lots/{lotId}' => %w[get put patch delete],
  '/api/v1/lots/{lotId}/bids' => ['post'],
  '/api/v1/internal/lots/{lotId}/settle' => ['post'],
  '/api/v1/internal/auctions/{auctionId}/settle' => ['post'],
  '/api/v1/payments' => ['post'],
  '/api/v1/payments/{paymentId}' => ['get']
}

required_routes.each do |route, methods|
  missing = methods.reject { |method| document.dig('paths', route, method) }
  errors << "#{route} is missing #{missing.join(', ')}" unless missing.empty?
end

if errors.empty?
  puts "OpenAPI contract is valid: #{path}"
else
  warn errors.map { |error| "- #{error}" }
  exit 1
end
