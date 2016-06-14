--tmr.alarm(0,2000,0,function() dofile('s0lrider.lua') end)
tmr.alarm(0,2000,0,function() dofile('s0lriderPebble.lua') end)

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