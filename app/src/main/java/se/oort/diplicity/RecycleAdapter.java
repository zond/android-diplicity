package se.oort.diplicity;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import se.oort.diplicity.apigen.SingleContainer;

public abstract class RecycleAdapter<T,VH extends RecycleAdapter.ViewHolder> extends RecyclerView.Adapter<VH> {

    private List<T> items;
    protected Context ctx;

    public abstract class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
        }
        public abstract void bind(T item, int pos);
    }

    public RecycleAdapter(Context ctx, List<T> items) {
        this.items = items;
        this.ctx = ctx;
    }

    public void clear() {
        int before = this.items.size();
        this.items.clear();
        notifyItemRangeRemoved(0, before);
    }

    public void addAll(List<T> games) {
        int before = this.items.size();
        this.items.addAll(games);
        notifyItemRangeInserted(before, games.size());
    }

    @Override
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public void onBindViewHolder(VH holder, int position) {
        T item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
