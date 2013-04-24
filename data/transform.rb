#!/usr/bin/env ruby

require 'csv'

file_name = ARGV[0]
if file_name.nil?
  puts "Usage: #{__FILE__} in.cvs >out.csv."
  exit 1
end

origin = CSV.read file_name

# We ignore the first line
header = origin[0]
rows = origin[1..-1]
h2i = {}
header.each_with_index do |h,i|
  h2i[h] = i
end

data = []
rows.each_with_index do |row, i|
  row_index = i + 2
  csv = []
  begin
    name = row[h2i['NAME']]
    desc = row[h2i['BESCHREIBUNG']]
    cat = row[h2i['SERIE']]
    sku = row[h2i['SKU']]
    price = row[h2i['PREIS']].to_i * 100
    currency = "EUR"
    image = row[h2i['URL']]
    image_label = 'foo'
    age = row[h2i['ALTER']]
    parts = row[h2i['TEILE']]
    csv = ['legoType', name, desc, cat, 'Custom tax category', '1', sku, price, currency, age, parts, image, image_label]
    data << csv
  rescue => e
    puts "[row #{row_index}]: problem occurred: #{e}"
    print e.backtrace.join("\n")
    exit 1
  end
end

head = %w'productType name description categories tax variantId sku centAmount currencyCode age parts images imageLabels'
puts head.to_csv
data.each do |r|
  puts r.to_csv
end
