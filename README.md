# CasualMode

A Paper 1.21.11 plugin that makes survival easier for the players you pick, and leaves it alone for everyone else.

You put players on a list. They take less damage, they heal when they're low, their dog is harder to kill, and their name turns green in the tab list. Everyone else keeps playing normal Minecraft right next to them.

How much easier is up to you. Half damage, a quarter, or (if you really want) nothing at all from mobs. It all lives in `config.yml`, and every number can be changed in-game.

## Installing

1. Put `casualmode-1.0.0.jar` in your server's `plugins` folder.
2. Restart the server. (Not `/reload`. Paper warns you about that one, and it's right.)
3. Add your players, either under `player-list` in `plugins/CasualMode/config.yml` or in-game with `/casualmode playerlist add <name>`.

You need Paper 1.21.11 and Java 21 or newer. It won't load on Spigot, because the commands use Paper's own command system.

## Damage

Every kind of damage has a `damage-received-percent`. It's the part of the hit that actually lands.

- `100` is vanilla.
- `50` is half.
- `0` is nothing.

Damage is split into groups:

| Group | Default | What's in it |
| --- | --- | --- |
| `mobs` | 50% | Zombies, spiders, bee stings, somebody else's wolf, a warden's sonic boom |
| `players` | off | Anything another player does to you: their sword, their arrows, their TNT |
| `fall` | off | Falling |
| `projectiles` | 50% | Arrows, tridents, fireballs, shulker bullets, llama spit |
| `explosives` | 50% | Creepers, TNT, end crystals, beds blowing up in the Nether |
| `magic` | 50% | Harming potions, poison, the Wither effect, evoker fangs, guardian lasers |

"Off" means that group stays completely vanilla. So with the defaults, PvP hurts exactly as much as it always did, and so does falling.

## AutoHeal

```yaml
auto-heal:
  enabled: true
  health-threshold: 10.00
  heal-amount: 2.00
  interval: 4.00
```


Here's what the defaults do. A casual player gets down to 7 HP. On the next tick they heal 2 HP, so they're at 9 HP. Four seconds later they're still below 10 HP, so they heal to 11 HP. That's above the threshold, so it stops. If they get hit again it starts over, but never more than once every 4 seconds. Logging out and back in doesn't reset that.

## Pets

Anything a casual player tamed gets their protection too: wolves, cats, parrots, horses, donkeys, mules, llamas, camels, nautiluses. Damage reduction and AutoHeal both apply. The colored name doesn't, because a wolf isn't in the tab list.

`pets.use-own-settings` decides which numbers pets use:

- `false` (default): the same settings as players.
- `true`: the `pets.settings` section instead. Same options, same names.

The `enabled` switches inside `pets.settings` are separate from the main toggles. Turn off AutoHeal for players and pets using their own settings keep healing.

## Name color

Casual players show up in the tab list in `color-name.color`, which is GREEN by default. Chat and the name above their head stay the same, and their actual username obviously doesn't change.

Take someone off the list and their tab name goes back to normal. If another plugin changed their tab name in the meantime, CasualMode leaves it alone instead of fighting over it.

Any of the 16 Minecraft colors work. BLACK works too, technically. Good luck reading it.

## Commands

Everything is under `/casualmode`. Tab completion suggests every option, sensible values, and player names, so you mostly won't need this table.

| Command | What it does | Who can use it |
| --- | --- | --- |
| `/casualmode set <setting> <value>` | Change a setting | Admins |
| `/casualmode toggle <system>` | Turn `damage-reduction`, `auto-heal`, `pets` or `color-name` on or off | Admins |
| `/casualmode playerlist add <player>` | Add a player | Admins |
| `/casualmode playerlist remove <player>` | Remove a player | Admins |
| `/casualmode playerlist get` | Show the list | Everyone |
| `/casualmode info` | Show every setting | Everyone |
| `/casualmode reload` | Reload `config.yml` | Admins |
| `/casualmode help` | Show the commands | Everyone |

The words after `set` are the same as the path in the config:

```
/casualmode set damage-reduction mobs damage-received-percent 25
/casualmode set damage-reduction players enabled true
/casualmode set auto-heal interval 2.5
/casualmode set pets use-own-settings true
/casualmode set pets settings auto-heal health-threshold 8
/casualmode set color-name color AQUA
```

Changes apply right away and get saved to `config.yml`.

`set` can't turn the four main systems on or off. That's `toggle`'s job, and `/casualmode toggle` on its own shows what's on right now.

If you edit `config.yml` by hand, run `/casualmode reload`.