'use strict'

const mineflayer = require('mineflayer')
const { Vec3 } = require('vec3')

const bot = mineflayer.createBot({
  host: process.env.MC_HOST || '127.0.0.1',
  port: Number(process.env.MC_PORT || 25565),
  username: 'FawesuiteTest',
  version: '1.21.11'
})

const delay = ms => new Promise(resolve => setTimeout(resolve, ms))
const messages = []
bot.on('messagestr', message => messages.push(message))

async function command (text, wait = 700) {
  bot.chat(text)
  await delay(wait)
}

function assertBlock (x, y, z, expected) {
  const block = bot.blockAt(new Vec3(x, y, z))
  if (!block || block.name !== expected) {
    throw new Error(`Expected ${expected} at ${x},${y},${z}; got ${block && block.name}`)
  }
}

bot.once('spawn', async () => {
  try {
    await delay(1000)
    await command('//pos1 0,100,0')
    await command('//pos2 1,100,0')
    await command('//set stone', 1800)
    await command('//multireplace stone dirt dirt gold_block', 2000)
    assertBlock(0, 100, 0, 'dirt')
    assertBlock(1, 100, 0, 'dirt')

    await command('//msel push')
    await command('//pos1 4,100,0')
    await command('//pos2 5,100,0')
    await command('//msel push')
    await command('//msel list -p 0')
    if (!messages.some(message => message.includes('Selection stack (page 0/0)'))) {
      throw new Error('Paginated msel list did not render')
    }
    await command('//msel combine')
    await command('//set diamond_block', 1200)
    assertBlock(0, 100, 0, 'diamond_block')
    assertBlock(1, 100, 0, 'diamond_block')
    assertBlock(4, 100, 0, 'diamond_block')
    assertBlock(5, 100, 0, 'diamond_block')

    await command('//ssel save -m integration')
    await command('//pos1 8,100,0')
    await command('//pos2 8,100,0')
    await command('//ssel load integration')
    await command('//set emerald_block', 1200)
    assertBlock(0, 100, 0, 'emerald_block')
    assertBlock(1, 100, 0, 'emerald_block')
    assertBlock(4, 100, 0, 'emerald_block')
    assertBlock(5, 100, 0, 'emerald_block')
    await command('//ssel delete integration')

    await command('//pos1 10,100,0')
    await command('//pos2 11,100,0')
    await command('//sc new paint //set ${1}')
    await command('//sc paint lapis_block', 1500)
    assertBlock(10, 100, 0, 'lapis_block')
    assertBlock(11, 100, 0, 'lapis_block')
    await command('//set stone', 1500)
    await command('//sc new #rocks stone,dirt')
    await command('//replace #rocks gold_block', 1500)
    assertBlock(10, 100, 0, 'gold_block')
    assertBlock(11, 100, 0, 'gold_block')

    await command('/tp @s 20.5 100 0.5')
    await command('//pin')
    await command('/tp @s 25.5 100 0.5')
    await command('//pos1')
    await command('//unpin')
    await command('//pos2')
    await command('//set red_wool', 1500)
    assertBlock(20, 100, 0, 'red_wool')
    assertBlock(25, 100, 0, 'red_wool')

    await command('//echo replace mud_brick_* air')
    if (!messages.some(message => message.includes('mud_brick_wall'))) {
      throw new Error('Echo did not expand mud_brick_*')
    }
    await command('//schematic search integration')
    if (!messages.some(message => message.includes('Schematic matches (page 0/0)'))) {
      throw new Error('Schematic fuzzy search did not render')
    }
    await command('//bmask clear')
    await command('//sc delete paint')
    await command('//sc delete #rocks')

    await command('//pos1 30,100,0')
    await command('//pos2 30,100,0')
    await command('//set stone', 1200)
    await command('//pos1 31,100,0')
    await command('//pos2 31,100,0')
    await command('//set glowstone', 1200)
    await command('//pos1 30,100,0')
    await command('//pos2 31,100,0')
    await command('//replace #emitslight[15][15] gold_block', 1500)
    assertBlock(30, 100, 0, 'stone')
    assertBlock(31, 100, 0, 'gold_block')
    await command('//replace #visible diamond_block', 1500)
    assertBlock(30, 100, 0, 'diamond_block')

    await command('//pos1 42,100,0')
    await command('//pos2 42,100,0')
    await command('//set emerald_block', 1200)
    await command('/tp @s 40.5 100 0.5')
    await command('//copynear -c emerald_block 5', 1800)
    await command('/tp @s 50.5 100 0.5')
    await command('//paste', 1500)
    assertBlock(50, 100, 0, 'emerald_block')

    await command('//pos1 60,100,0')
    await command('//pos2 60,100,0')
    await command('//set stone', 1200)
    await command('//repeat 1 east', 1500)
    assertBlock(61, 100, 0, 'stone')

    await command('//tpsel ~ ~5 ~')
    if (!messages.some(message => message.includes('Teleported to 60, 105, 0.'))) {
      throw new Error(`Unexpected tpsel result: ${bot.entity.position}`)
    }

    console.log('PASS editing, selection, persistence, masks, clipboard, aliases, pin, echo, and teleport workflows')
    bot.quit('integration complete')
    process.exitCode = 0
  } catch (error) {
    console.error(error.stack || error)
    console.error(messages.slice(-20).join('\n'))
    bot.quit('integration failed')
    process.exitCode = 1
  }
})

bot.on('kicked', reason => console.error('Kicked:', reason))
bot.on('error', error => {
  console.error(error.stack || error)
  process.exitCode = 1
})
