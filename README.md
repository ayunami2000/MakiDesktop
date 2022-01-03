# MakiScreen Mjpeg
> 📺 Streaming OBS video/Mjpeg into maps on item frames at a high frame rate

![demo1](https://github.com/ayunami2000/MakiScreen-Mjpeg/raw/master/images/2021-12-31_17.13.21.png)
![demo2](https://github.com/ayunami2000/MakiScreen-Mjpeg/raw/master/images/2021-12-31_17.48.58.png)
*images taken on TotalFreedom: play.totalfreedom.me*

## How does it work

- Load jar plugin onto 1.17.1 Paper server
- It will connect to URL specified in the config.yml file as an mjpeg stream
- Use [cam2web](https://github.com/cvsandbox/cam2web/releases) to send [OBS Virtual Camera](https://www.youtube.com/watch?v=bfrknjDzukI) as JPG frames to the HTTP port
- 
- Renders the latest available frame in Minecraft! 

## Get started (It's decently easy, and somewhat experimental)

**Beware that map will probably be overwritten**

- Download [MakiScreen](https://github.com/ayunami2000/MakiScreen-Mjpeg/actions) jar and place in Spigot 1.13+ server plugins
- Download [cam2web](https://github.com/cvsandbox/cam2web/releases) and make sure it's in your path (optional)
- Download [OBS](https://obsproject.com) (optional)
- Port forward the port that cam2web uses (if server is not the same as the one streaming) (this port is customizable)
- Run **Paper** server, and then turn it off after the server has finished starting up
- Change the ***size*** setting in config.yml to match your output resolution
- Change the ***url*** setting in config.yml to match your mjpeg url (cam2web default: http://127.0.0.1:8000/camera/mjpeg)
- Run **OBS** and make sure the output resolution is according to the config option and the base resolution is set to same as the config or any resolution with 2:1 aspect ratio
- Turn on your ***OBS Virtual Camera***
- Run cam2web, choose ***OBS Virtual Camera***, and choose the smaller of the two resolutions. Then, press "Start streaming."
- Run **Paper** server and Type `/maki give` in Minecraft to get the maps

if your performance went doodoo, you could try removing both data.yml in the MakiScreen folder and removing anything in the data folder in the world folder.
you can also try `/maki clear` out and then restart the server. This simply clears data.yml

## Help me

You can contact the original author on **Discord** at [Maki#4845](https://maki.cat/discord) or on **Twitter** at [@MakiXx_](https://twitter.com/MakiXx_)
You can contact me on **Discord** at `ayunami2000#5250` or on **Twitter** at `@noThnxCya`

## Credit
- [CodedRed](https://www.youtube.com/channel/UC_kPUW3XPrCCRT9a4Pnf1Tg) For ImageManager class
- [DNx5](https://github.com/dnx5) for synchronizing the maps, optimizing the code, implementing sierra2 dithering. literally do all the hard work for me
- [EzMediaCore](https://github.com/MinecraftMediaLibrary/EzMediaCore) for the dither algorithm
- [MJPG](https://github.com/Wildcats3540/Dashboard/tree/master/Dashboard/src/com/wildcatrobotics/dashboard/MJPG)
- [MakiScreen](https://github.com/makitsune/MakiScreen)