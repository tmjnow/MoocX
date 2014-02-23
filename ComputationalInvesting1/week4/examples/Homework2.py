'''
(c) 2011, 2012 Georgia Tech Research Corporation
This source code is released under the New BSD license.  Please see
http://wiki.quantsoftware.org/index.php?title=QSTK_License
for license details.

Created on January, 23, 2013

@author: Sourabh Bajaj
@contact: sourabhbajaj@gatech.edu
@summary: Event Profiler Tutorial
'''


import pandas as pd
import numpy as np
import math
import copy
import QSTK.qstkutil.qsdateutil as du
import datetime as dt
import QSTK.qstkutil.DataAccess as da
import QSTK.qstkutil.tsutil as tsu
import QSTK.qstkstudy.EventProfiler as ep

"""
Accepts a list of symbols along with start and end date
Returns the Event Matrix which is a pandas Datamatrix
Event matrix has the following structure :
    |IBM |GOOG|XOM |MSFT| GS | JP |
(d1)|nan |nan | 1  |nan |nan | 1  |
(d2)|nan | 1  |nan |nan |nan |nan |
(d3)| 1  |nan | 1  |nan | 1  |nan |
(d4)|nan |  1 |nan | 1  |nan |nan |
...................................
...................................
Also, d1 = start date
nan = no information about any event.
1 = status bit(positively confirms the event occurence)
"""


def find_events(ls_symbols, d_data):
    ''' Finding the event dataframe '''
    df_close = d_data['actual_close']
    ts_market = df_close['SPY']

    print "Finding Events"

    # Creating an empty dataframe i.e. df=datafreame
    df_events = copy.deepcopy(df_close)
    df_events = df_events * np.NAN

    # Time stamps for the event range
    ldt_timestamps = df_close.index

    for s_sym in ls_symbols:
        for i in range(1, len(ldt_timestamps)):
            # Calculating the returns for this timestamp
            f_symprice_today = df_close[s_sym].ix[ldt_timestamps[i]]
            f_symprice_yest = df_close[s_sym].ix[ldt_timestamps[i - 1]]
            f_marketprice_today = ts_market.ix[ldt_timestamps[i]]
            f_marketprice_yest = ts_market.ix[ldt_timestamps[i - 1]]
            f_symreturn_today = (f_symprice_today / f_symprice_yest) - 1
            f_marketreturn_today = (f_marketprice_today / f_marketprice_yest) - 1

            # Event is found if the symbol is down more then 3% while the
            # market is up more then 2%
            if f_symreturn_today <= -0.03 and f_marketreturn_today >= 0.02:
                df_events[s_sym].ix[ldt_timestamps[i]] = 1

    return df_events


if __name__ == '__main__':
    dt_start = dt.datetime(2008, 1, 1)
    dt_end = dt.datetime(2009, 12, 31)
    ldt_timestamps = du.getNYSEdays(dt_start, dt_end, dt.timedelta(hours=16))

    dataobj = da.DataAccess('Yahoo')
    ls_symbols1 = dataobj.get_symbols_from_list('sp5002008')
    ls_symbols2 = dataobj.get_symbols_from_list('sp5002012')
    
    ls_symbols1.append('SPY')
    ls_symbols2.append('SPY')

    ls_keys = ['open', 'high', 'low', 'close', 'volume', 'actual_close']
    ldf_data1 = dataobj.get_data(ldt_timestamps, ls_symbols1, ls_keys)
    ldf_data2= dataobj.get_data(ldt_timestamps, ls_symbols2, ls_keys)
    
    c = dict(zip(ls_keys, ldf_data1))
    d_data1 = dict(zip(ls_keys, ldf_data1))
    
    c2 = dict(zip(ls_keys, ldf_data2))
    d_data2 = dict(zip(ls_keys, ldf_data2))

    df_events1 = find_events(ls_symbols1, d_data1)
    df_events2 = find_events(ls_symbols2, d_data2)
    print "Creating Study"
    ep.eventprofiler(df_events1, d_data1, i_lookback=20, i_lookforward=20,
                s_filename='MyEventStudy1.pdf', b_market_neutral=True, b_errorbars=True,
                s_market_sym='SPY')
    ep.eventprofiler(df_events2, d_data2, i_lookback=20, i_lookforward=20,
                s_filename='MyEventStudy2.pdf', b_market_neutral=True, b_errorbars=True,
                s_market_sym='SPY')
