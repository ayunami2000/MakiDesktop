# MakiDesktop
> 📺 Controlling VNC through a Minecraft server

![demo1](https://github.com/ayunami2000/MakiDesktop/raw/master/images/2022-01-02_22.05.29.png)
![demo2](https://github.com/ayunami2000/MakiDesktop/raw/master/images/2022-01-02_22.44.46.png)

## How does it work

- Load jar plugin onto 1.17.1 Paper server
- It will connect to IP:PORT specified in the config.yml file **must have no authentication (for now)
- Renders the latest available frame in Minecraft! 

## Get started (It's super easy to set up, and pretty easy to use)

**Beware that map will probably be overwritten**

- Download [MakiDesktop](https://github.com/ayunami2000/MakiDesktop/actions) jar and place in Spigot 1.13+ server plugins
- Run **Paper** server
- Change the ***size*** setting in config.yml or using /makid to match your output resolution
- Change the ***ip*** setting in config.yml or using /makid to match your VNC IP:PORT. Run `/makid toggle` to update the IP:PORT internally.
- Run **Paper** server and Type `/makid give` in Minecraft to get the maps
- Face the top left corner of the maps area, and run `/makid loc` to set the screen location.
- Type `/makid ctrl` to start controlling!

if your performance went doodoo, you could try removing both data.yml in the MakiScreen folder and removing anything in the data folder in the world folder.

you can also try `/makid clear` out and then restart the server. This simply clears data.yml

## Help me

You can contact the original author on **Discord** at [Maki#4845](https://maki.cat/discord) or on **Twitter** at [@MakiXx_](https://twitter.com/MakiXx_)

You can contact me on **Discord** at `ayunami2000#5250` or on **Twitter** at `@noThnxCya`

## Credit
- [CodedRed](https://www.youtube.com/channel/UC_kPUW3XPrCCRT9a4Pnf1Tg) For ImageManager class
- [DNx5](https://github.com/dnx5) for synchronizing the maps, optimizing the code, implementing sierra2 dithering. literally do all the hard work for me
- [EzMediaCore](https://github.com/MinecraftMediaLibrary/EzMediaCore) for the dither algorithm
- [MakiScreen](https://github.com/makitsune/MakiScreen)
- [Vernacular VNC](https://github.com/shinyhut/vernacular-vnc)