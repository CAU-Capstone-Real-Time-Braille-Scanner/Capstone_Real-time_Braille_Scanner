import os
os.environ['KMP_DUPLICATE_LIB_OK']='True'

from web_app import angelina_reader_app
angelina_reader_app.run()
