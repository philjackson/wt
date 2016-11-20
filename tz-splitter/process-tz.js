var https = require('https')
var fs = require('fs')

function mkdirMaybe(dir) {
  if (!fs.existsSync(dir)){
    fs.mkdirSync(dir)
  }
}

function validateLens(zone) {
  // we need these to correspond as it's how we strip useless data
  if ((zone.abbrs.length != zone.untils.length) || (zone.untils.length != zone.offsets.length)) {
    throw new Error("Seems like there's a data mismatch for " + zone.name)
  }
}

function beginSplit(all) {
  let parsed = JSON.parse(all)

  mkdirMaybe(parsed.version)

  for (zone of parsed.zones) {
    let [continent, city] = zone.name.split("/")
    validateLens(zone)

    let path = parsed.version + "/" + continent 
    mkdirMaybe(path)

    for (until of zone.untils) {
      
    }

    fs.writeFileSync(path + "/" + city, JSON.stringify(zone))
  }
}

var request = https.get("https://raw.githubusercontent.com/moment/moment-timezone/develop/data/unpacked/latest.json", (res) => {
  let all = "";

  res.on('data', (d) => { all += d })
  res.on('end', () => { beginSplit(all) })
})
