# ShopBlock Plugin Overview

ShopBlock introduces a standalone, placeable shop block that lets players sell items without tying the experience to beacon mechanics. Server staff can distribute the custom block via `/shopblock give`, and players configure their offer by sneaking and right-clicking the placed block.

## Player Experience

### Creating a Shop
1. Obtain a **Shop Block** from staff (or yourself if you have `shopblock.admin`).
2. Place the block where you want the vending machine to live.
3. Sneak and right-click the block to open the editor.
4. Put a sample of the product you want to sell in the left slot, the payment stack in the right slot, and deposit additional product copies into the center slot to seed stock. Saving consumes the deposited items and adds them to the shop's inventory.

### Buying From a Shop
1. Right-click the block (no sneaking required).
2. Review the display to see the product, cost, and remaining stock.
3. Click the green concrete to confirm. The plugin withdraws the payment, gives you the product, and updates stock.
4. If the shop empties out, it automatically locks until the owner restocks.

### Managing a Shop
- Owners can withdraw earnings directly from the editor using the gold ingot button.
- Breaking the block as the owner (or with `shopblock.admin.break`) refunds remaining stock, any unclaimed earnings, and the shop block item itself so it can be moved.
- Configuration persists across restarts via a YAML data file. Earnings stay banked until the owner collects them.

## Administration & Permissions
- `shopblock.admin`: Allows using `/shopblock give` to distribute Shop Blocks.
- `shopblock.admin.break`: Lets staff break another player's shop without owning it.

## Requirements
- Paper (or compatible) server on Minecraft 1.20+.
- Java 21, aligned with the Gradle build provided in this repository.
- Either `curl` or `wget` on the build machine so the Gradle wrapper script can fetch its bootstrap JAR on demand (or run `gradle wrapper` locally to generate it beforehand).

Future integrations—such as requiring the block to sit within a beacon aura—can hook into the stored block positions without altering the core shop logic.
