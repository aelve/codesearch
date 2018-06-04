require "json"

specs = Marshal.load(Gem.gunzip(File.read(ARGV[0])))
File.open(ARGV[1], "w") do |f| 
  f.write(specs.to_json)
end
