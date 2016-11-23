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
  let recently = (new Date()).getTime() - 86400000 // now - 24 hours

  mkdirMaybe(parsed.version)

  for (zone of parsed.zones) {
    let [continent, city] = zone.name.split("/")
    validateLens(zone)

    let path = parsed.version + "/" + continent
    mkdirMaybe(path)

    let earliest = null

    for (i in zone.untils) {
      earliest = i

      if (zone.untils[i] > recently) {
        break
      }
    }

    let new_zone = []

    // here we're removing the offsets etc that are in the past -
    // we don't need to know them as we only care about the future...
    if (earliest != null) {
      for (let j = earliest; j < zone.untils.length; j++) {
        new_zone.push([zone.untils[j], zone.abbrs[j], zone.offsets[j]])
      }
    }

    fs.writeFileSync(path + "/" + city, JSON.stringify(new_zone))
  }
}

if (process.argv[2]) {
  beginSplit(fs.readFileSync(process.argv[2]))
}
else {
  var request = https.get("https://raw.githubusercontent.com/moment/moment-timezone/develop/data/unpacked/latest.json", (res) => {
    let all = "";

    res.on('data', (d) => { all += d })
    res.on('end', () => { beginSplit(all) })
  })
}
