//
// S0lRider Pebble App v1
// Daniel Casado de Luis
// May 2016
//

#include <pebble.h>
#define TUPLE_KEY 0

Window *window;
TextLayer *text_layer;
// declaring the dictation objects
static DictationSession *s_dictation_session;
static char s_last_text[40];
static TextLayer *s_output_layer;


// declaring background image stuff
static GBitmap *s_bitmap;
static BitmapLayer *s_bitmap_layer;

//Send command to android app
static void send(int key, char *value) {
  DictionaryIterator *iter;
  app_message_outbox_begin(&iter);

  dict_write_cstring(iter, key, value);

  app_message_outbox_send();
}
// Handlers function for success on sending a command to android app
static void outbox_sent_handler(DictionaryIterator *iter, void *context) {
  // Display command received
  static char s_received_buff[50];
  Tuple *t = dict_read_first(iter);
  if(t) {
    dict_read_first(iter);
    snprintf(s_received_buff, sizeof(s_received_buff), "Order received: %s", t->value->cstring);
  }
  text_layer_set_text(s_output_layer, s_received_buff );
}

// Handler fucntion fail attempt when sending to android app
static void outbox_failed_handler(DictionaryIterator *iter, AppMessageResult reason, void *context) {
  text_layer_set_text(s_output_layer, "Sorry Michael, communication error!");
  APP_LOG(APP_LOG_LEVEL_ERROR, "Fail reason: %d", (int)reason);
}

//callback function to handle dictation process
static void dictation_session_callback(DictationSession *session, DictationSessionStatus status, 
                                       char *msg, void *context) {
  int check = -1;
  if(status == DictationSessionStatusSuccess) {
    // Display the dictated text
    //snprintf(s_last_text, sizeof(s_last_text), "Alright boss moving %s", transcription);
    //text_layer_set_text(s_output_layer, s_last_text);
    if(strcmp(msg, "Up") == 0) { check=1; }
    if(strcmp(msg, "Down") == 0) { check=1; }
    if(strcmp(msg, "Left") == 0) { check=1; } 
    if(strcmp(msg, "Right") == 0) { check=1; }
    
    if (check == 1) { 
      send(TUPLE_KEY, msg);  
    } else {
      text_layer_set_text(s_output_layer, "Sorry Michael Knight: up, down, left or right?");
    }
  } else {
    // Display the reason for any error
    static char s_failed_buff[128];
    snprintf(s_failed_buff, sizeof(s_failed_buff), "Speak louder Michael! ;)\nError ID:%d", (int)status);
    text_layer_set_text(s_output_layer, s_failed_buff);
  }
}

//callback function/handler that gets called when the user presses the middle button
static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  // Start voice dictation UI
  dictation_session_start(s_dictation_session);
}

//callback function/handler when user presses the down button on the right
static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  // tell the car to stop (typically after sending a voice command)
  send(TUPLE_KEY,"center");
}


//registration of buttons for dictation and for stopping car
static void click_config_provider(void *context) {
  //register the select button to start recording
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
  //register the lower/down button to stop car
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

//accelerometer stuff: tapping or wrist shake
static void accel_tap_handler(AccelAxisType axis, int32_t direction) {
  //Eeaster egg :)
  send(TUPLE_KEY, "lights");
}

//function that gets called when the windows is loaded per the init function declaration
static void window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);
  // Add the image layer
  s_bitmap_layer = bitmap_layer_create(GRect(0, 0, 144, 168));
  bitmap_layer_set_compositing_mode(s_bitmap_layer, GCompOpSet);
  bitmap_layer_set_bitmap(s_bitmap_layer, s_bitmap);
  // add the image layer before the dictation
  layer_add_child(window_layer, bitmap_layer_get_layer(s_bitmap_layer));
  //centering the dictation layer
  //s_output_layer = text_layer_create(GRect(bounds.origin.x, (bounds.size.h - 24) / 2, bounds.size.w, bounds.size.h));
  s_output_layer = text_layer_create(GRect(bounds.origin.x, (bounds.size.h - 50), bounds.size.w, bounds.size.h));
  text_layer_set_text(s_output_layer, "Press middle button for voice commands: up,down,left,right");
  text_layer_set_text_alignment(s_output_layer, GTextAlignmentCenter);
  //add title * S0L RIDER *
    // Create the TextLayer, for display at (0, 0),
  // and with a size of 144 x 40
  text_layer = text_layer_create(GRect(23, 8, 144, 20));
  // Set the text that the TextLayer will display
  text_layer_set_text(text_layer, "*** s0lRider ***");
  // Add as child layer to be included in rendering
  layer_add_child(window_layer, text_layer_get_layer(text_layer));
  // add the dictation text layer child
  layer_add_child(window_layer, text_layer_get_layer(s_output_layer));
}


static void window_unload(Window *window) {
  //added the title layer
  text_layer_destroy(text_layer);
  text_layer_destroy(s_output_layer);
}

//initialization function
void init(){
  // Create the Window
  window = window_create();
  // image background initialization
  s_bitmap = gbitmap_create_with_resource(RESOURCE_ID_SOLEBMP_IMAGE);
  
  // Enabling registering actions for dictation (select button) and stop car (down button)
  window_set_click_config_provider(window, click_config_provider);
  window_set_window_handlers(window, (WindowHandlers) {
    .load = window_load,
    .unload = window_unload,
  });
  // Push to the stack, animated
  window_stack_push(window, true);

  // Create new dictation session
  s_dictation_session = dictation_session_create(sizeof(s_last_text), dictation_session_callback, NULL);
  
  // Disable the confirmation screen after speaking (annoying for our purposes)
  dictation_session_enable_confirmation(s_dictation_session, false);
  
  // Initialize messaging stuff
  app_message_register_outbox_sent(outbox_sent_handler);
  app_message_register_outbox_failed(outbox_failed_handler);
  
  const int inbox_size = 128;
  const int outbox_size = 128;
  app_message_open(inbox_size, outbox_size);
  
  // Accelerometer call back function registering
  accel_tap_service_subscribe(accel_tap_handler);
}

void deinit(){
   // Destroy accelerator binding
  accel_tap_service_unsubscribe();
  // Destroy the TextLayer
  text_layer_destroy(text_layer);
  // Destroy the image layer stuff
  gbitmap_destroy(s_bitmap);
  bitmap_layer_destroy(s_bitmap_layer);
  // Destroy dictation stuff
  text_layer_destroy(s_output_layer);
  dictation_session_destroy(s_dictation_session);
  // Destroy the Window
  window_destroy(window);
}

int main() {
  // Initialize the app
  init();

  // Wait for app events
  app_event_loop();

  // Deinitialize the app
  deinit();

  // App finished without error
  return 0;
}
