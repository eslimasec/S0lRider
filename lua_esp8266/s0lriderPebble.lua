--S0lrider LUA Pebble version, use the standalone version if not using Pebble.
--wifi stuff
--Listening port of UDP server to receive commdands from phone
port=7777
essid="s0lrider"
wifipw="********"

--scanner lights speed in milliseconds
scanDelay = 130000


function scannerLights()
--did this trick to loop through non consecutive numbers (leds numbers)
        local scan = {8,7,6,5,0}
        for i,led in ipairs(scan) do
            gpio.write(led,gpio.HIGH)
            --milliseconds
            tmr.delay(scanDelay)
            gpio.write(led,gpio.LOW)
        end
        local scanback = {5,6,7,8}
        for i,led in ipairs(scanback) do
            gpio.write(led,gpio.HIGH)
            --milliseconds
            tmr.delay(scanDelay)
            gpio.write(led,gpio.LOW)
        end
end

--gpio stuff
gpio.mode(0,gpio.OUTPUT)
gpio.mode(1,gpio.OUTPUT)
gpio.mode(2,gpio.OUTPUT)
gpio.mode(3,gpio.OUTPUT)
gpio.mode(4,gpio.OUTPUT)
gpio.mode(5,gpio.OUTPUT)
gpio.mode(6,gpio.OUTPUT)
gpio.mode(7,gpio.OUTPUT)
gpio.mode(8,gpio.OUTPUT)

--turning off state to all leds and motors
gpio.write(0,gpio.LOW)
gpio.write(1,gpio.LOW)
gpio.write(2,gpio.LOW)
gpio.write(3,gpio.LOW)
gpio.write(4,gpio.LOW)
gpio.write(5,gpio.LOW) 
gpio.write(6,gpio.LOW)
gpio.write(7,gpio.LOW)
gpio.write(8,gpio.LOW)

-- Station mode: accepts voice commands via Pebble, you have to create a hotstpot
-- ...with your phone so that esp8266 connects to it with the parameters below
    wifi.setmode(wifi.STATION)
    wifi.sta.config(essid,wifipw)
    tmr.alarm(0, 3000, 1, function()   
    print("Trying to connect to wifi network: "..essid.." with password: "..wifipw)
    ip=wifi.sta.getip()
        if ip ~= nil then         
            tmr.stop(0)
            print("IP:"..ip..", Port:"..port)         
            print ("S0lRider Pebble version starting")
            --light scanner lights to show s0lrider is ready
            scannerLights()
            
            s=net.createServer(net.UDP) 
            s:on("receive",function(s,c) 
                --print(c)
                if     c == "up" then 
                    print("Forward")
                    gpio.write(1,gpio.HIGH)
                    gpio.write(2,gpio.LOW)
                    gpio.write(3,gpio.LOW)
                    gpio.write(4,gpio.HIGH)
            
                elseif c == "down" then 
                    print("Backward")
                    gpio.write(1,gpio.LOW)
                    gpio.write(2,gpio.HIGH)
                    gpio.write(3,gpio.HIGH)
                    gpio.write(4,gpio.LOW)
                elseif c == "right" then 
                    print("Right")
                    gpio.write(1,gpio.HIGH)
                    gpio.write(2,gpio.HIGH)
                    gpio.write(3,gpio.LOW)
                    gpio.write(4,gpio.LOW)
                elseif c == "left" then 
                    print("Left")
                    gpio.write(1,gpio.LOW)
                    gpio.write(2,gpio.LOW)
                    gpio.write(3,gpio.HIGH)
                    gpio.write(4,gpio.HIGH)
            
                elseif c == "center" then 
                    print("Stop")
                    gpio.write(1,gpio.LOW)
                    gpio.write(2,gpio.LOW)
                    gpio.write(3,gpio.LOW)
                    gpio.write(4,gpio.LOW)  
                elseif c == "lights" then 
                    print("Lights On")
                    scannerLights()
                end
            end)
            s:listen(port)
        end 
        
    end) 
